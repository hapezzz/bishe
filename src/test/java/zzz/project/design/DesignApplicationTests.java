package zzz.project.design;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.DigestUtils;
import zzz.project.design.entity.User;

@SpringBootTest
class DesignApplicationTests {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Test
    void contextLoads() {
//        redisTemplate.opsForValue().set("test","test");
//        User user = new User ();
//        user.setIdentify("ls");
//        user.setUser_name("www");
//        redisTemplate.opsForZSet().add("users",user,1);
    }


    @Test
    void get (){
        User user = new User();
        user.setUser_name("admin");
        user.setAccount("admin");
        user.setPassword(DigestUtils.md5DigestAsHex("zhaoxt".getBytes()));
        user.setBirthday("1999-09-09");
        user.setUser_age(21);
        user.setEmail("test@test.com");
        user.setManager(2);
        redisTemplate.opsForZSet().add("users",user,0);
//        redisTemplate.opsForHash().put("accounts","admin",0);
//        redisTemplate.opsForHash().put("emails","test@test.com",0);
    }

    @Test
    void put(){
        redisTemplate.opsForHash().put("countoflogin","2020-04-07",9);
    }
}
