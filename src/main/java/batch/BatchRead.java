package batch;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * 总结：需要对Redis进行批量操作时，建议使用Redis 管道（Pipeline），这样可以大幅提升性能

 redis.clients.jedis.exceptions.JedisConnectionException: java.net.ConnectException: Connection refused: connect
 解决：保证windows下可以telnet ip 6307
     (1).redis.conf中将属性protected-mode设置为no，默认为yes，redis开启了保护模式
     (2).注释掉redis.conf文件中的如下一行，默认redis只能在本机访问
     # bind 127.0.0.1
     (3).重新启动Redis服务，连接成功!
 */
public class BatchRead {

    private static final String HOST = "192.168.191.65";
    private static final int PORT = 6379;

    // 批量从Redis中获取数据，正常使用
    public static Map<String, String> batchGetNotUsePipeline() throws Exception {
        Map<String, String> map = new HashMap<String, String>();

        Jedis jedis = new Jedis(HOST, PORT);

        String keyPrefix = "normal";
        long begin = System.currentTimeMillis();
        for (int i = 1; i < 10000; i++) {
            String key = keyPrefix + "_" + i;
            String value = jedis.get(key);
            map.put(key, value);
        }
        jedis.close();

        long end = System.currentTimeMillis();
        System.out.println("not use pipeline batch get total time：" + (end - begin));
        return map;
    }

    // 批量从Redis中获取数据，使用Pipeline
    public static Map<String, String> batchGetUsePipeline() throws Exception {
        Map<String, String> map = new HashMap<String, String>();

        Jedis jedis = new Jedis(HOST, PORT);
        Pipeline pipelined = jedis.pipelined();
        String keyPrefix = "pipeline";

        // 使用pipeline方式批量获取数据，只能获取到value值，对应的key获取不到，我们通过一个中间map来获取key
        HashMap<String, Response<String>> intrmMap = new HashMap<String, Response<String>>();
        long begin = System.currentTimeMillis();
        for (int i = 1; i < 10000; i++) {
            String key = keyPrefix + "_" + i;
            intrmMap.put(key, pipelined.get(key));
        }

        pipelined.sync();
        jedis.close();

        for (Map.Entry<String, Response<String>> entry :intrmMap.entrySet()) {
            Response<String> sResponse = (Response<String>)entry.getValue();
            String key = new String(entry.getKey());
            String value = sResponse.get();
            map.put(key, value);
        }

        long end = System.currentTimeMillis();
        System.out.println("use pipeline batch get total time：" + (end - begin));
        return map;
    }

    public static void main(String[] args) {
        try {
            batchGetNotUsePipeline();
            batchGetUsePipeline();

//          Map<String, String> normalMap = batchGetNotUsePipeline();
//          for(Map.Entry<String, String> entry : normalMap.entrySet()) {
//              System.out.println(entry.getKey() + "=" + entry.getValue());
//          }

//          Map<String, String> pipelineMap = batchGetUsePipeline();
//          for(Map.Entry<String, String> entry : pipelineMap.entrySet()) {
//              System.out.println(entry.getKey() + "=" + entry.getValue());
//          }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
