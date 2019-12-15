package sharding;
import com.alibaba.fastjson.JSON;
import model.UserDO;
import org.junit.jupiter.api.Test;

/**
 *  测试  Sharding 独立redis 客户端
 */
public class ShardingSimpleClient {

    @Test
    public void userCache(){

        //向缓存中保存对象
        UserDO user = UserDO.builder().id(22347).sex(0).age(20).name("张三3").unick("zhangsan3")
                .email("zhangsan3@yahoo.com").build();;
        //调用方法处理
        boolean reusltCache =  ShardingRedisClient.set("zhangsan3", user);
        if (reusltCache) {
            System.out.println("向缓存中保存对象成功。");
        }else{
            System.out.println("向缓存中保存对象失败。");
        }
    }


    @Test
    public void getUserInfo(){
        String zhuoxuan = (String)ShardingRedisClient.get("zhangsan3");
        UserDO user = JSON.parseObject(zhuoxuan, UserDO.class);
        if(zhuoxuan != null){
            System.out.println("从缓存中获取的对象，" + user.getName() + "===="+user.getEmail());
        }
    }

}
