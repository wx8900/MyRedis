package batch;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class BatchWrite {

    private static final String HOST = "192.168.191.65";
    private static final int PORT = 6379;

    // 批量插入数据到Redis，正常使用
    public static void batchSetNotUsePipeline() throws Exception {
        Jedis jedis = new Jedis(HOST, PORT);
        String keyPrefix = "normal";
        long begin = System.currentTimeMillis();
        for (int i = 1; i < 10000; i++) {
            String key = keyPrefix + "_" + i;
            String value = String.valueOf(i);
            jedis.set(key, value);
        }
        jedis.close();
        long end = System.currentTimeMillis();
        System.out.println("not use pipeline batch set total time：" + (end - begin));
    }

    // 批量插入数据到Redis，使用Pipeline
    public static void batchSetUsePipeline() throws Exception {
        Jedis jedis = new Jedis(HOST, PORT);
        Pipeline pipelined = jedis.pipelined();
        String keyPrefix = "pipeline";
        long begin = System.currentTimeMillis();
        for (int i = 1; i < 10000; i++) {
            String key = keyPrefix + "_" + i;
            String value = String.valueOf(i);
            pipelined.set(key, value);
        }
        pipelined.sync();
        jedis.close();
        long end = System.currentTimeMillis();
        System.out.println("use pipeline batch set total time：" + (end - begin));
    }

    public static void main(String[] args) {
        try {
            batchSetNotUsePipeline();
            batchSetUsePipeline();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
