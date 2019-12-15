import model.UserDO;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import sharding.RedisShardingClient;


public class RedisClientTest {

    //保存对象
    @Test
    public void userCache(){
        //向缓存中保存对象
        UserDO test = UserDO.builder()
                .id(777)
                .name("test qq")
                .age(80)
                .sex(1)
                .unick("log qq")
                .email("logtest@qq.com")
                .build();
        //调用方法处理
        boolean reusltCache = RedisClient.set("QQ", test);
        if (reusltCache) {
            System.out.println("向缓存中保存对象成功。");
        }else{
            System.out.println("向缓存中保存对象失败。");
        }
    }

    //获取对象
    @Test
    public void getUserInfo(){
        UserDO jin = (UserDO)RedisClient.get("QQ");
        if(jin != null){
            System.out.println("从缓存中获取的对象，" + jin.getName());
            System.out.println(jin.getAge());
            System.out.println(jin.getEmail());
        }
    }

    @Test
    public void clientRedis() {

        ApplicationContext context = new ClassPathXmlApplicationContext("spring-common.xml");

        RedisShardingClient client = context.getBean(RedisShardingClient.class);

        // 向缓存中保存对象
        UserDO test = UserDO.builder()
                .id(2156)
                .name("test log")
                .age(29)
                .sex(0)
                .unick("log log")
                .email("logtest@yahoo.com")
                .build();
        // 调用方法处理
        boolean reusltCache = client.set("LogTest", test);
        if (reusltCache) {
            System.out.println("向缓存中保存对象成功。");
        } else {
            System.out.println("向缓存中保存对象失败。");
        }

        UserDO lisi = client.get("LogTest", UserDO.class);
        if (lisi != null) {
            System.out.println("从缓存中获取的对象，" + lisi.getName() + "  @  " + lisi.getEmail());
        }

    }

}
