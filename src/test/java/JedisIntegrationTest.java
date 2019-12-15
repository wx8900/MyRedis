import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.*;
import redis.embedded.RedisServer;

public class JedisIntegrationTest {

    private static Jedis jedis;
    private static RedisServer redisServer;
    private static int port;

    @BeforeClass
    public static void setUp() throws IOException {

        // Take an available port
        ServerSocket s = new ServerSocket(0);
        port = s.getLocalPort();
        s.close();

        redisServer = new RedisServer(port);
        redisServer.start();

        // Configure JEDIS
        jedis = new Jedis("localhost", port);
    }

    @AfterClass
    public static void destroy() {
        redisServer.stop();
    }

    @After
    public void flush() {
        jedis.flushAll();
    }

    @Test
    public void givenAString_thenSaveItAsRedisStrings() {
        String key = "key";
        String value = "value";

        jedis.set(key, value);
        String value2 = jedis.get(key);

        Assert.assertEquals(value, value2);
    }

    /**
    redis的事务很简单，他主要目的是保障，一个client发起的事务中的命令可以连续的执行，而中间不会插入其他client的命令
     调用jedis.watch(…)方法来监控key，如果调用后key值发生变化，则整个事务会执行失败。另外，事务中某个操作失败，并不会回滚其他操作。
     这一点需要注意。还有，我们可以使用discard()方法来取消事务
     */
    @Test
    public void test2Trans() {
        Jedis jedis = new Jedis("localhost");
        long start = System.currentTimeMillis();
        Transaction tx = jedis.multi();
        for (int i = 0; i < 100000; i++) {
            tx.set("t" + i, "t" + i);
        }
        List<Object> results = tx.exec();
        long end = System.currentTimeMillis();

        System.out.println("Transaction SET: " + ((end - start)/1000.0) + " seconds");    jedis.disconnect();
    }

    /**
     *  有时，我们需要采用异步方式，一次发送多个指令，不同步等待其返回结果。这样可以取得非常好的执行效率。这就是管道
     */
    @Test
    public void test3Pipelined() {
        Jedis jedis = new Jedis("localhost");
        Pipeline pipeline = jedis.pipelined();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            pipeline.set("p" + i, "p" + i);
        }
        List<Object> results = pipeline.syncAndReturnAll();
        long end = System.currentTimeMillis();
        System.out.println("Pipelined SET: " + ((end - start)/1000.0) + " seconds");
        jedis.disconnect();
    }

    /**
     *  就Jedis提供的方法而言，是可以做到在管道中使用事务
     *  但是经测试（见本文后续部分），发现其效率和单独使用事务差不多，甚至还略微差点
     */
    @Test
    public void test4combPipelineTrans() {
        jedis = new Jedis("localhost");
        long start = System.currentTimeMillis();
        Pipeline pipeline = jedis.pipelined();
        pipeline.multi();
        for (int i = 0; i < 100000; i++) {
            pipeline.set("" + i, "" + i);
        }    pipeline.exec();
        List<Object> results = pipeline.syncAndReturnAll();
        long end = System.currentTimeMillis();
        System.out.println("Pipelined transaction: " + ((end - start)/1000.0) + " seconds");
        jedis.disconnect();
    }

    /**
     *  分布式直连同步调用
     *  这个是分布式直接连接，并且是同步调用，每步执行都返回执行结果。类似地，还有异步管道调用
     */
    @Test
    public void test5shardNormal() {
        List<JedisShardInfo> shards = Arrays.asList(
                new JedisShardInfo("localhost",6379),
                new JedisShardInfo("localhost",6380));

        ShardedJedis sharding = new ShardedJedis(shards);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            String result = sharding.set("sn" + i, "n" + i);
        }
        long end = System.currentTimeMillis();
        System.out.println("Simple@Sharing SET: " + ((end - start)/1000.0) + " seconds");

        sharding.disconnect();
    }

    /**
     * 分布式直连异步调用
     */
    @Test
    public void test6shardpipelined() {
        List<JedisShardInfo> shards = Arrays.asList(
                new JedisShardInfo("localhost",6379),
                new JedisShardInfo("localhost",6380));

        ShardedJedis sharding = new ShardedJedis(shards);

        ShardedJedisPipeline pipeline = sharding.pipelined();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            pipeline.set("sp" + i, "p" + i);
        }
        List<Object> results = pipeline.syncAndReturnAll();
        long end = System.currentTimeMillis();
        System.out.println("Pipelined@Sharing SET: " + ((end - start)/1000.0) + " seconds");

        sharding.disconnect();
    }

    /**
     * 分布式连接池同步调用
     * 如果，你的分布式调用代码是运行在线程中，那么上面两个直连调用方式就不合适了，因为直连方式是非线程安全的，这个时候，你就必须选择连接池调用
     */
    @Test
    public void test7shardSimplePool() {
        List<JedisShardInfo> shards = Arrays.asList(
                new JedisShardInfo("localhost",6379),
                new JedisShardInfo("localhost",6380));

        ShardedJedisPool pool = new ShardedJedisPool(new JedisPoolConfig(), shards);

        ShardedJedis one = pool.getResource();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            String result = one.set("spn" + i, "n" + i);
        }
        long end = System.currentTimeMillis();
        pool.returnResource(one);
        System.out.println("Simple@Pool SET: " + ((end - start)/1000.0) + " seconds");

        pool.destroy();
    }

    /**
     * 分布式连接池异步调用
     * 分布式中，连接池方式调用不但线程安全外，根据上面的测试数据，也可以看出连接池比直连的效率更好。
     * 需要注意的地方:
     *   (1) 事务和管道都是异步模式。在事务和管道中不能同步查询结果
     *   (2) 事务和管道都是异步的，个人感觉，在管道中再进行事务调用，没有必要，不如直接进行事务模式。
     *   (3) 分布式中，连接池的性能比直连的性能略好(见后续测试部分)。
     *   (4) 分布式调用中不支持事务。因为事务是在服务器端实现，而在分布式中，每批次的调用对象都可能访问不同的机器，所以，没法进行事务
     */
    @Test
    public void test8shardPipelinedPool() {
        List<JedisShardInfo> shards = Arrays.asList(
                new JedisShardInfo("localhost",6379),
                new JedisShardInfo("localhost",6380));

        ShardedJedisPool pool = new ShardedJedisPool(new JedisPoolConfig(), shards);

        ShardedJedis one = pool.getResource();

        ShardedJedisPipeline pipeline = one.pipelined();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            pipeline.set("sppn" + i, "n" + i);
        }
        List<Object> results = pipeline.syncAndReturnAll();
        long end = System.currentTimeMillis();
        pool.returnResource(one);
        System.out.println("Pipelined@Pool SET: " + ((end - start)/1000.0) + " seconds");
        pool.destroy();
    }











    @Test
    public void givenListElements_thenSaveThemInRedisList() {
        String queue = "queue#tasks";

        String taskOne = "firstTask";
        String taskTwo = "secondTask";
        String taskThree = "thirdTask";

        jedis.lpush(queue, taskOne, taskTwo);

        String taskReturnedOne = jedis.rpop(queue);

        jedis.lpush(queue, taskThree);
        Assert.assertEquals(taskOne, taskReturnedOne);

        String taskReturnedTwo = jedis.rpop(queue);
        String taskReturnedThree = jedis.rpop(queue);

        Assert.assertEquals(taskTwo, taskReturnedTwo);
        Assert.assertEquals(taskThree, taskReturnedThree);

        String taskReturnedFour = jedis.rpop(queue);
        Assert.assertNull(taskReturnedFour);
    }

    @Test
    public void givenSetElements_thenSaveThemInRedisSet() {
        String countries = "countries";

        String countryOne = "Spain";
        String countryTwo = "Ireland";
        String countryThree = "Ireland";

        jedis.sadd(countries, countryOne);

        Set<String> countriesSet = jedis.smembers(countries);
        Assert.assertEquals(1, countriesSet.size());

        jedis.sadd(countries, countryTwo);
        countriesSet = jedis.smembers(countries);
        Assert.assertEquals(2, countriesSet.size());

        jedis.sadd(countries, countryThree);
        countriesSet = jedis.smembers(countries);
        Assert.assertEquals(2, countriesSet.size());

        boolean exists = jedis.sismember(countries, countryThree);
        Assert.assertTrue(exists);
    }

    @Test
    public void givenObjectFields_thenSaveThemInRedisHash() {
        String key = "user#1";

        String field = "name";
        String value = "William";

        String field2 = "job";
        String value2 = "politician";

        jedis.hset(key, field, value);
        jedis.hset(key, field2, value2);

        String value3 = jedis.hget(key, field);
        Assert.assertEquals(value, value3);

        Map<String, String> fields = jedis.hgetAll(key);
        String value4 = fields.get(field2);
        Assert.assertEquals(value2, value4);
    }

    @Test
    public void givenARanking_thenSaveItInRedisSortedSet() {
        String key = "ranking";

        Map<String, Double> scores = new HashMap<>();

        scores.put("PlayerOne", 3000.0);
        scores.put("PlayerTwo", 1500.0);
        scores.put("PlayerThree", 8200.0);

        scores.keySet().forEach(player -> {
            jedis.zadd(key, scores.get(player), player);
        });

        Set<String> players = jedis.zrevrange(key, 0, 1);
        Assert.assertEquals("PlayerThree", players.iterator().next());

        long rank = jedis.zrevrank(key, "PlayerOne");
        Assert.assertEquals(1, rank);
    }

    @Test
    public void givenMultipleOperationsThatNeedToBeExecutedAtomically_thenWrapThemInATransaction() {
        String friendsPrefix = "friends#";

        String userOneId = "4352523";
        String userTwoId = "5552321";

        Transaction t = jedis.multi();
        t.sadd(friendsPrefix + userOneId, userTwoId);
        t.sadd(friendsPrefix + userTwoId, userOneId);
        t.exec();

        boolean exists = jedis.sismember(friendsPrefix + userOneId, userTwoId);
        Assert.assertTrue(exists);

        exists = jedis.sismember(friendsPrefix + userTwoId, userOneId);
        Assert.assertTrue(exists);
    }

    @Test
    public void givenMultipleIndependentOperations_whenNetworkOptimizationIsImportant_thenWrapThemInAPipeline() {
        String userOneId = "4352523";
        String userTwoId = "4849888";

        Pipeline p = jedis.pipelined();
        p.sadd("searched#" + userOneId, "paris");
        p.zadd("ranking", 126, userOneId);
        p.zadd("ranking", 325, userTwoId);
        Response<Boolean> pipeExists = p.sismember("searched#" + userOneId, "paris");
        Response<Set<String>> pipeRanking = p.zrange("ranking", 0, -1);
        p.sync();

        Assert.assertTrue(pipeExists.get());
        Assert.assertEquals(2, pipeRanking.get().size());
    }

    @Test
    public void givenAPoolConfiguration_thenCreateAJedisPool() {
        final JedisPoolConfig poolConfig = buildPoolConfig();

        try (JedisPool jedisPool = new JedisPool(poolConfig, "localhost", port); Jedis jedis = jedisPool.getResource()) {

            // do simple operation to verify that the Jedis resource is working
            // properly
            String key = "key";
            String value = "value";

            jedis.set(key, value);
            String value2 = jedis.get(key);

            Assert.assertEquals(value, value2);

            // flush Redis
            jedis.flushAll();
        }
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }
}
