//import net.sf.json.JSONObject;
import com.alibaba.fastjson.JSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;

/**
 * Created on 2016/6/14.
 *
 * https://github.com/ZhuoxuanOwen/technology-solution/tree/master/02-%E7%BC%93%E5%AD%98%E6%96%B9%E6%A1%88%E5%8F%8ANoSQL
 */

public class RedisClient {

    public static JedisPool jedisPool; // 池化管理jedis链接池

    static {
        //读取相关的配置
        ResourceBundle resourceBundle = ResourceBundle.getBundle("redis"); // read redis.properties
        int maxActive = Integer.parseInt(resourceBundle.getString("redis.pool.maxActive"));
        int maxIdle = Integer.parseInt(resourceBundle.getString("redis.pool.maxIdle"));
        int maxWait = Integer.parseInt(resourceBundle.getString("redis.pool.maxWait"));

        String ip = resourceBundle.getString("redis.ip");
        int port = Integer.parseInt(resourceBundle.getString("redis.port"));

        JedisPoolConfig config = new JedisPoolConfig();
        //设置最大连接数
        config.setMaxTotal(maxActive);
        //设置最大空闲数
        config.setMaxIdle(maxIdle);
        //设置超时时间
        config.setMaxWaitMillis(maxWait);
        //初始化连接池
        jedisPool = new JedisPool(config, ip, port);
    }

    /**
     * 向缓存中设置字符串内容
     *
     * @param key   key
     * @param value value
     * @return
     * @throws Exception
     */
    public static boolean set(String key, String value) throws Exception {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /**
     * 向缓存中设置对象
     *
     * @param key
     * @param value
     * @return
     */
    public static boolean set(String key, Object value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.set(key.getBytes(), SerializeUtil.serialize(value));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /**
     * 删除缓存中得对象，根据key
     *
     * @param key
     * @return
     */
    public static boolean del(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.del(key);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /**
     * 根据key 获取内容
     *
     * @param key
     * @return
     */
    public static Object get(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[] value = jedis.get(key.getBytes());
            return SerializeUtil.unserialize(value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /**
     * 根据key 获取对象T
     *
     * @param //key
     * @param //clazz
     * @param //<T>
     * @return
     */
//    public static <T> T get(String key,Class<T> clazz){
//        Jedis jedis = null;
//        try {
//            jedis = jedisPool.getResource();
//            String value = jedis.get(key);
//            return JSON.parseObject(value, clazz);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }finally{
//            jedisPool.returnResource(jedis);
//        }
//    }

    public static void main(String[] args) throws Exception {
//        //测试存储字符串
//        RedisClient.set("hello", "world");
//        System.out.println(RedisClient.get("hello"));
//        //测试存储对象
//        People people = new People();// 存放对象时 必须实现序列化
//        people.setName("wamhf");
//        people.setAge(26);
//        RedisClient.set("name1", people);
//        People p = (People) RedisClient.get("name1");
//        System.out.println(p.getAge() + "-------" + p.getName());
//        //测试存储List
//        List list = new ArrayList();
//        list.add("111");
//        list.add("222");
//        list.add("333");
//        RedisClient.set("list", list);
//        List list1 = (List) RedisClient.get("list");
//        for (Object o : list1) {
//            System.out.println(o.toString());
//        }
//        //测试存储Set
//        Set set = new HashSet<>();
//        set.add("a");
//        set.add(1);
//        RedisClient.set("set", set);
//        Set s = (Set) RedisClient.get("set");
//        for (Object o : s) {
//            System.out.println(o.toString());
//        }
//        System.out.println("---------------------验证set去重---------------------");
//        Set set1 = new HashSet<>();
//        set1.add("a");
//        set1.add(1);
//        RedisClient.set("set", set1);
//        Set s1 = (Set) RedisClient.get("set");
//        for (Object o : s1) {
//            System.out.println(o.toString());
//        }

        RedisClient redisClient = new RedisClient();
        redisClient.test();
    }

    public void test() {
        Jedis redis = jedisPool.getResource();
        /* -----------------------------------------------------------------------------------------------------------  */
        // KEY操作

        //KEYS
        Set keys = redis.keys("*");//列出所有的key，查找特定的key如：redis.keys("foo")
        Iterator t1 = keys.iterator();
        while (t1.hasNext()) {
            Object obj1 = t1.next();
            System.out.println(obj1);
        }

        //DEL 移除给定的一个或多个key。如果key不存在，则忽略该命令。
//        redis.del("set");

        //TTL 返回给定key的剩余生存时间(time to live)(以秒为单位)
        System.out.println(redis.ttl("hello"));

        //PERSIST key 移除给定key的生存时间。
        redis.persist("foo");

        //EXISTS 检查给定key是否存在。
        redis.exists("foo");

        //MOVE key db  将当前数据库(默认为0)的key移动到给定的数据库db当中。如果当前数据库(源数据库)和给定数据库(目标数据库)有相同名字的给定key，或者key不存在于当前数据库，那么MOVE没有任何效果。
        redis.move("foo", 1);//将foo这个key，移动到数据库1

        //RENAME key newkey  将key改名为newkey。当key和newkey相同或者key不存在时，返回一个错误。当newkey已经存在时，RENAME命令将覆盖旧值。
        //redis.rename("foo", "foonew");

        //TYPE key 返回key所储存的值的类型。
        System.out.println(redis.type("foo"));//none(key不存在),string(字符串),list(列表),set(集合),zset(有序集),hash(哈希表)

        //EXPIRE key seconds 为给定key设置生存时间。当key过期时，它会被自动删除。
        redis.expire("foo", 5);//5秒过期
        //EXPIREAT EXPIREAT的作用和EXPIRE一样，都用于为key设置生存时间。不同在于EXPIREAT命令接受的时间参数是UNIX时间戳(unix timestamp)。

        //一般SORT用法 最简单的SORT使用方法是SORT key。
        redis.lpush("sort", "1");
        redis.lpush("sort", "4");
        redis.lpush("sort", "6");
        redis.lpush("sort", "3");
        redis.lpush("sort", "0");

        List list = redis.sort("sort");//默认是升序
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }


      /* -----------------------------------------------------------------------------------------------------------  */
        // STRING 操作

        //SET key value将字符串值value关联到key。
        redis.set("name", "wangjun1");
        redis.set("id", "123456");
        redis.set("address", "guangzhou");

        //SETEX key seconds value将值value关联到key，并将key的生存时间设为seconds(以秒为单位)。
        redis.setex("foo", 5, "haha");

        //MSET key value [key value ...]同时设置一个或多个key-value对。
        redis.mset("haha", "111", "xixi", "222");

        //redis.flushAll();清空所有的key
        System.out.println(redis.dbSize());//dbSize是多少个key的个数

        //APPEND key value如果key已经存在并且是一个字符串，APPEND命令将value追加到key原来的值之后。
        redis.append("foo", "00");//如果key已经存在并且是一个字符串，APPEND命令将value追加到key原来的值之后。

        //GET key 返回key所关联的字符串值
        redis.get("foo");

        //MGET key [key ...] 返回所有(一个或多个)给定key的值
        List list1 = redis.mget("haha", "xixi");
        for (int i = 0; i < list1.size(); i++) {
            System.out.println(list1.get(i));
        }

        //DECR key将key中储存的数字值减一。
        //DECRBY key decrement将key所储存的值减去减量decrement。
        //INCR key 将key中储存的数字值增一。
        //INCRBY key increment 将key所储存的值加上增量increment。


        /* -----------------------------------------------------------------------------------------------------------  */
        // Hash 操作

        //HSET key field value将哈希表key中的域field的值设为value。
        redis.hset("website", "google", "www.google.cn");
        redis.hset("website", "baidu", "www.baidu.com");
        redis.hset("website", "sina", "www.sina.com");

        //HMSET key field value [field value ...] 同时将多个field - value(域-值)对设置到哈希表key中。
        Map map = new HashMap();
        map.put("cardid", "123456");
        map.put("username", "jzkangta");
        redis.hmset("hash", map);

        //HGET key field返回哈希表key中给定域field的值。
        System.out.println(redis.hget("hash", "username"));

        //HMGET key field [field ...]返回哈希表key中，一个或多个给定域的值。
        List list2 = redis.hmget("website", "google", "baidu", "sina");
        for (int i = 0; i < list2.size(); i++) {
            System.out.println(list2.get(i));
        }

        //HGETALL key返回哈希表key中，所有的域和值。
        Map<String, String> map1 = redis.hgetAll("hash");
        for (Map.Entry entry : map1.entrySet()) {
            System.out.print(entry.getKey() + ":" + entry.getValue() + "\t");
        }

        //HDEL key field [field ...]删除哈希表key中的一个或多个指定域。
        //HLEN key 返回哈希表key中域的数量。
        //HEXISTS key field查看哈希表key中，给定域field是否存在。
        //HINCRBY key field increment为哈希表key中的域field的值加上增量increment。
        //HKEYS key返回哈希表key中的所有域。
        //HVALS key返回哈希表key中的所有值。


        /* -----------------------------------------------------------------------------------------------------------  */
        //  LIST 操作
        //LPUSH key value [value ...]将值value插入到列表key的表头。
        redis.lpush("list", "abc");
        redis.lpush("list", "xzc");
        redis.lpush("list", "erf");
        redis.lpush("list", "bnh");

        //LRANGE key start stop返回列表key中指定区间内的元素，区间以偏移量start和stop指定。下标(index)参数start和stop都以0为底，也就是说，以0表示列表的第一个元素，以1表示列表的第二个元素，以此类推。你也可以使用负数下标，以-1表示列表的最后一个元素，-2表示列表的倒数第二个元素，以此类推。
        List list3 = redis.lrange("list", 0, -1);
        for (int i = 0; i < list3.size(); i++) {
            System.out.println(list3.get(i));
        }

        //LLEN key返回列表key的长度。
        //LREM key count value根据参数count的值，移除列表中与参数value相等的元素。

        /* -----------------------------------------------------------------------------------------------------------  */
        //  SET 操作
        //SADD key member [member ...]将member元素加入到集合key当中。
        redis.sadd("testSet", "s1");
        redis.sadd("testSet", "s2");
        redis.sadd("testSet", "s3");
        redis.sadd("testSet", "s4");
        redis.sadd("testSet", "s5");

        //SREM key member移除集合中的member元素。
        redis.srem("testSet", "s5");

        //SMEMBERS key返回集合key中的所有成员。
        Set set = redis.smembers("testSet");
        Iterator t2 = set.iterator();
        while (t2.hasNext()) {
            Object obj1 = t2.next();
            System.out.println(obj1);
        }

        //SISMEMBER key member判断member元素是否是集合key的成员。是（true），否则（false）
        System.out.println(redis.sismember("testSet", "s4"));

        //SCARD key返回集合key的基数(集合中元素的数量)。
        //SMOVE source destination member将member元素从source集合移动到destination集合。

        //SINTER key [key ...]返回一个集合的全部成员，该集合是所有给定集合的交集。
        //SINTERSTORE destination key [key ...]此命令等同于SINTER，但它将结果保存到destination集合，而不是简单地返回结果集
        //SUNION key [key ...]返回一个集合的全部成员，该集合是所有给定集合的并集。
        //SUNIONSTORE destination key [key ...]此命令等同于SUNION，但它将结果保存到destination集合，而不是简单地返回结果集。
        //SDIFF key [key ...]返回一个集合的全部成员，该集合是所有给定集合的差集 。
        //SDIFFSTORE destination key [key ...]此命令等同于SDIFF，但它将结果保存到destination集合，而不是简单地返回结果集。


    }

}