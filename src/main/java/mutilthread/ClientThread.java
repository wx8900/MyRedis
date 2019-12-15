package mutilthread;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 在单机4核普通PC机器测试下来10000条数据跑了2秒，性能还是不错的，没有报异常。

 下面是一个处理不好的情况，就会报出种种异常，这种连接池一定要用多线程测试，不然线下没事，线上就会时不时的出问题
 */
public class ClientThread extends Thread {

    int i = 0;

    public ClientThread(int i) {
        this.i = i;
    }

    public void run() {
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(date);
        JedisUtil.setString("foo", time);
        String foo = JedisUtil.getString("foo");
        //System.out.println("【输出>>>>】foo:" + foo + " 第："+i+"个线程" +"当前时间："+ DateUtil.getNowTimeString());
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            ClientThread t = new ClientThread(i);
            t.start();
        }
    }

}
