import lombok.Cleanup;
import lombok.SneakyThrows;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
//import rfx.core.nosql.jedis.RedisCommand;
//import rfx.core.util.DateTimeUtil;

/**
 *  打开Redis数据库命令：
 *  cd /Users/Jack_Cai/redis-4.0.11
 *  src/redis-server
 *  redis-cli
 *  redis-cli shutdown
 *
 *  检测后台进程是否存在
 *  ps -ef |grep redis
 *  检测6379端口是否在监听
 *  netstat -lntp | grep 6379
 *
 * 在分布式开发中，Redisson可提供更便捷的方法。
 * https://blog.csdn.net/u011489043/article/details/78796095
 * https://blog.csdn.net/qq_39478853/article/details/76154943
 * https://blog.csdn.net/qq616138361/article/details/79088212
 *
 * redis的应用场景如下：缓存（数据查询、短连接、新闻内容、商品内容等等）、分布式集群架构中的session分离、
 * 聊天室的在线好友列表、任务队列。（秒杀、抢购、12306等等）、应用排行榜、网站访问统计、数据过期处理（可以精确到毫秒）。
 * 其中，作为缓存的应用场景是最多的
 *
 * Redis的图形界面客户端软件，redis-destop-manager, 软件名称简写： rdm
 */
public class ConnectRedisUtils {

    public static void main(String[] args) {
        //Jedis jedis = connectRedis("localhost", 6379); // none-pool connect

        String password = null;
        final JedisPoolConfig poolConfig = buildPoolConfig("localhost", 6379, Protocol.DEFAULT_TIMEOUT, 128, password);
        @Cleanup
        JedisPool jedisPool = new JedisPool(poolConfig, "localhost"); // pool connect

        // Jedis implements Closeable. Hence, the jedis instance will be auto-closed after the last statement.
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("=========================> Connect Redis pool successfully!");
            storeString(jedis, "Redis tutorial");
            storeRedisList(jedis);
            searchListbyKey(jedis,"*"); // *-list
            searchSetbyKey(jedis,"nicknames");
            searchSortedSets(jedis, "PlayerTwo");
            searchHashesbyKey(jedis, "user#1");
            searchSetbyKey(jedis,"admin2019062211");
            transactions(jedis);
            pipelining(jedis);
            subscribe(jedis);
            //redisCluster();
            searchbyKeyfromPool(jedisPool, "*-list");
            deleteData(jedis, "Redis tutorial");
            jedis.set("tutorial-test-expire", "some time later will expire");
            testExpire(jedis, "tutorial-test-expire");
        } catch (JedisException je) {
            // logger.error(je.getMessage());
            throw new JedisConnectionException("RedisConfig.this.toString()", je);
        } catch (Exception e) {
            // logger.error(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (!jedisPool.isClosed()) {
                jedisPool.close();
            }
        }

    }

    private static Jedis connectRedis(String ipAddress, int port) {
        Jedis jedis = new Jedis(ipAddress, port);
        //jedis.auth("password");
        System.out.println("=====================> Connection to server successfully!");
        //check whether server is running or not
        System.out.println("Server is running: "+jedis.ping());
        return jedis;
    }

    private static void storeString(Jedis jedis, String str) {
        //set the data in redis string
        jedis.set("tutorial-name", str);
        jedis.set("foo", str + " test " + str);
        // Get the stored data and print it
        System.out.println("Stored string in redis : "+ jedis.get("tutorial-name"));
    }

    private static void storeRedisList(Jedis jedis) {
        //store data in redis list
        jedis.lpush("tutorial-list", "Redis");
        jedis.lpush("tutorial-list", "Mongodb");
        jedis.lpush("tutorial-list", "Mysql");
        jedis.lpush("tutorial-list", "Oracle");
        String task = jedis.rpop("tutorial-list");
        System.out.println("Stored the first element in list  :  "+ task); // 为什么这个值是随机的，不是一个固定值？
        // Get the stored data and print it
        List<String> list = jedis.lrange("tutorial-list", 0 ,5);

        for(int i = 0; i<list.size(); i++) {
            System.out.println("Stored List in redis  ::  "+list.get(i));
        }
    }

    @SneakyThrows(JedisConnectionException.class)
    private static String searchbyKeyfromPool(JedisPool redisPool, String keyName) {
        @Cleanup
        JedisPool myPool = redisPool;
        @Cleanup
        Jedis redis = myPool.getResource();
        return redis.get(keyName);
    }

    private static void searchListbyKey(Jedis jedis, String keyPattern) {
        // Get the stored data and print it
        Set<String> set = jedis.keys(keyPattern);
        for(int i = 0; i< set.size(); i++) {
            System.out.println("search List by key (" + keyPattern + ") :: "+ (set.toArray())[i]);
        }
    }

    private static void searchSetbyKey(Jedis jedis, String key) {
        jedis.sadd("nicknames", "Xiao Ming");
        jedis.sadd("nicknames", "Wang Gang");
        jedis.sadd("nicknames", "Li Hai");

        Set<String> nicknames = jedis.smembers(key);
        boolean exists = jedis.sismember(key, "Wang");
        System.out.println("Does the key (" + key + ") exists in Set? " + exists);
    }

    // Sorted Sets are like a Set where each member has an associated ranking, that is used for sorting them
    // Order : DESC
    private static void searchSortedSets(Jedis jedis, String member) {
        Map<String, Double> scores = new HashMap<>();
        scores.put("PlayerOne", 3000.0);  // 1
        scores.put("PlayerTwo", 1500.0);  // 2
        scores.put("PlayerThree", 8200.0);// 0

        scores.keySet().forEach(player -> {
            jedis.zadd("ranking", scores.get(player), player);
        });

        String player = jedis.zrevrange("ranking", 0, 1).iterator().next();
        long rank = jedis.zrevrank("ranking", member);
        System.out.println("The rank of " + member + " is : " + rank);
    }

    private static void searchHashesbyKey(Jedis jedis, String key) {
        jedis.hset("user#1", "name", "Peter");
        jedis.hset("user#1", "job", "politician");

        String name = jedis.hget("user#1", "name");
        System.out.println("Username is : " + name);
        Map<String, String> fields = jedis.hgetAll(key);
        String job = fields.get("job");
        System.out.println("Does this field exists? " + job);
    }

    private static void transactions(Jedis jedis) {
        String friendsPrefix = "friends#";
        String userOneId = "4352523";
        String userTwoId = "5552321";

        Transaction t = jedis.multi();
        t.sadd(friendsPrefix + userOneId, userTwoId);
        t.sadd(friendsPrefix + userTwoId, userOneId);

        System.out.println(" transaction 1 ====>" + jedis.watch("friends#deleted#" + userTwoId));
        System.out.println(" transaction 2 ====>" + jedis.watch("friends#deleted# " + userOneId));
        t.exec();
    }

    /**
     * 有时，我们需要采用异步方式，一次发送多个指令，不同步等待其返回结果。这样可以取得非常好的执行效率。这就是管道
     * @param jedis
     */
    private static void pipelining(Jedis jedis) {
        String userOneId = "1252523";
        String userTwoId = "9849888";

        Pipeline p = jedis.pipelined();
        p.sadd("searched#" + userOneId, "paris");
        p.zadd("ranking", 126, userOneId); // zadd 比 sadd 增加一个double参数score
        p.zadd("ranking", 325, userTwoId);
        Response<Boolean> pipeExists = p.sismember("searched#" + userOneId, "paris");
        Response<Set<String>> pipeRanking = p.zrange("ranking", 0, -1);
        p.sync();

        Boolean exists = pipeExists.get();
        Set<String> ranking = pipeRanking.get();
        System.out.println("watch exists ==========>" + exists + ", ranking is ASC order ==========>" + ranking);
    }

    // Subscribe is a blocking method
    private static void subscribe(Jedis jSubscriber) {
        jSubscriber.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                System.out.println("11111111111111111111");
                // handle message
            }
        }, "channel");

        String subscribe = jSubscriber.get("channel");
        System.out.println("watch subscribe ==========>" + subscribe);
        Jedis jPublisher = new Jedis("localhost", 6379);
        jPublisher.publish("channel", "test message");
        System.out.println("watch publish ==========>" + jPublisher.get("channel"));
    }

    private static JedisPoolConfig buildPoolConfig(String host, int port, int timeout, int resources, String password) {
        final JedisPoolConfig pConf = new JedisPoolConfig();
        pConf.setMaxWaitMillis(timeout); // 30000
        pConf.setMaxTotal(resources); // setMaxTotal(128);
        pConf.setMaxIdle(5);  // setMaxIdle(128);
        pConf.setMinIdle(1);  // setMinIdle(16);
        pConf.setTestOnBorrow(true);
        pConf.setTestOnReturn(true);
        pConf.setTestWhileIdle(true);
        pConf.setNumTestsPerEvictionRun(10);  // 3
        pConf.setTimeBetweenEvictionRunsMillis(60000); // 60000 ->Duration.ofSeconds(30).toMillis()
        pConf.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        pConf.setBlockWhenExhausted(true);
        return pConf;
    }

    private static void redisCluster() {
        Set<HostAndPort> jedisClusterNodes = new HashSet<>();
        //Jedis Cluster will attempt to discover cluster nodes automatically
        jedisClusterNodes.add(new HostAndPort("localhost", 6379)); //  if the master is on the same PC which runs your code
        jedisClusterNodes.add(new HostAndPort("192.168.1.35", 6379));
        System.out.println(" redisCluster =========> " + jedisClusterNodes.toArray()[0] +" ," +
                " " + jedisClusterNodes.toArray()[1]);

        // redis.clients.jedis.exceptions.JedisDataException: ERR This instance has cluster support disabled
        // ===> 修改配置  redis.conf  配置集群  修改redis.config，添加cluster-enabled yes或者去掉注释，重启redis即可
        try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes)) {
            // use the jedisCluster resource as if it was a normal Jedis resource
            jedisCluster.set("foo", "bar");
            jedisCluster.set("foo", "test");
            jedisCluster.set("dog", "Google");
            String value = jedisCluster.get("foo");
            System.out.println(" redisCluster =========> " + value);
        } catch (IOException e) {
            // logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void deleteData(Jedis jedis, String key1) {
        long count = jedis.del(key1);
        System.out.println("Get Data Key1 after it is deleted:" + jedis.get(key1));
    }

    public static void testExpire(Jedis jedis, String key2) {
        long count = jedis.expire(key2, 5);
        try {
            Thread.currentThread().sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (jedis.exists(key2)) {
            System.out.println("Get Key2 in Expire Action:" + jedis.scard(key2));
        } else {
            System.out.println("Key2 is expired with value:"+ jedis.scard(key2));
        }
    }


    /*
    public static boolean updateKafkaLogEvent(int unixtime, final String event){
        Date date = new Date(unixtime*1000L);
        final String dateStr = DateTimeUtil.formatDate(date ,DATE_FORMAT_PATTERN);
        final String dateHourStr = DateTimeUtil.formatDate(date ,YYYY_MM_DD_HH);

        boolean commited = new RedisCommand<Boolean>(jedisPool) {
            @Override
            protected Boolean build() throws JedisException {
                String keyD = MONITOR_PREFIX+dateStr;
                String keyH = MONITOR_PREFIX+dateHourStr;

                Pipeline p = jedis.pipelined();
                p.hincrBy(keyD, "e:"+event , 1L);
                p.expire(keyD, AFTER_4_DAYS);
                p.hincrBy(keyH, "e:"+event , 1L);
                p.expire(keyH, AFTER_2_DAYS);
                p.sync();
                return true;
            }
        }.execute();

        return commited;
    }
    */

}
