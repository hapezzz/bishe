package zzz.project.design.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import zzz.project.design.dao.UserDao;
import zzz.project.design.entity.TableResult;
import zzz.project.design.entity.User;
import zzz.project.design.tool.Email;
import zzz.project.design.tool.Tool;
import zzz.project.design.tool.VerifyCode;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserDao userDao;
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String from;

    public int sendVUserC(String to,String name) {
        String verify = VerifyCode.VerifyCode();
        int total = 15;
        long timeout = 180;
        long left = 24 * 3600;
        if (redisTemplate.hasKey(to + "-total")) {
            left = redisTemplate.getExpire(to + "-total", TimeUnit.SECONDS);
            total = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForValue().get(to + "-total")), Integer.class);
            if (total <= 0) {
                return -1;
            }
            total --;
            if (left < 180) {
                timeout = left;
            }
        }
        redisTemplate.opsForValue().set(to + "-total", total);
        redisTemplate.opsForValue().set(to + "-verify", verify);
        redisTemplate.expire(to + "-total", left, TimeUnit.SECONDS);
        redisTemplate.expire(to + "-verify", timeout, TimeUnit.SECONDS);

        String subject = "验证用户邮箱";
        String content = "<html lang='en'><head><meta charset='utf-8'></meta></head><body>用户"+name+":您此次的验证码是：<br>"+verify+"<br>输入时注意大小写，此验证码有效期为"+Tool.unit2time(timeout)+"，在接下来"+Tool.unit2time(left)+"还可以发送"+total+"次</body></html>";
        Email.sendEmail("1448787060@qq.com", subject, content, javaMailSender, from);
        return 1;
    }

    public int addTempUser(Map<String, Object> map) {
        if (userDao.uniqueAccount((String) map.get("account"))) {
            if (userDao.uniqueEmail((String) map.get("email"))) {
                User user = Tool.map2bean(map, User.class);
                double score = userDao.getScore();
                redisTemplate.opsForZSet().add("tempUsers", user, score);
                sendVUserC(user.getEmail(),user.getUser_name());
                redisTemplate.opsForHash().put("emails",user.getEmail(),score);
                redisTemplate.opsForHash().put("accounts",user.getAccount(),score);
                return 1;
            } else {
                return -2;//邮箱不唯一
            }
        } else {
            return -1;//帐号不唯一
        }
    }

    public int addUser(String email){
        double score = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForHash().get("emails",email)),Double.class);
        User user = null;
        Set<Object> set = redisTemplate.opsForZSet().rangeByScore("tempUsers",score,score);
        for(Object temp:set){
            user = JSONObject.parseObject(JSONObject.toJSONString(temp),User.class);
            redisTemplate.opsForZSet().remove("tempUsers",user);
        }
        if(user==null){
            return -1;
        }
        redisTemplate.opsForZSet().add("users",user,score);
        redisTemplate.opsForHash().put("numofadd",user.getAccount(),user.getAccount());
        return 1;
    }

    public TableResult viewUsers(String query_key,String temp,long limit,long offset){
        String table = "users";
        if("temp".equals(temp)){
            table = "tempUsers";
        }
        TableResult tableResult = new TableResult();
        LinkedHashSet<User> users = new LinkedHashSet<>();
        Set<Object> set = new HashSet<>();
        long count = 0;
        if(Tool.isNull(query_key)){
            double max = userDao.getScore();
            set = redisTemplate.opsForZSet().rangeByScore(table,0,max,offset,limit);
            count = redisTemplate.opsForZSet().zCard(table);
        }else{
            ScanOptions scanOptions = ScanOptions.scanOptions().match("*"+query_key+"*").build();
            Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet().scan("users", scanOptions);
            while(cursor.hasNext()){
                ZSetOperations.TypedTuple<Object> typedTuple = cursor.next();
                set.add(typedTuple.getValue());
                count++;
            }
            log.debug("query1");
        }
        if(set==null){
            return null;
        }else{
            tableResult.setCount(count);
            for(Object object:set){
                User user = JSONObject.parseObject(JSONObject.toJSONString(object),User.class);
                users.add(user);
            }
            tableResult.setData(users);
        }
        return tableResult;
    }

    public int resetProp(String account, Map<Integer, Object> map) {
        int key = 0;String value = "";
        Iterator<Map.Entry<Integer,Object>> iterator = map.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer,Object> entry = iterator.next();
            key = entry.getKey();
            value = (String) entry.getValue();
        }
        User user = userDao.getUser(account);
        double score = redisTemplate.opsForZSet().score("users",user);
        switch (key){
            case 1:
                user.setUser_name(value);
                break;
            case 2:
                user.setUser_age(Integer.parseInt(value));
                break;
            case 3:
                user.setUser_sex(value);
                break;
            case 4:
                user.setBirthday(value);
                break;
            case 5:
                user.setEmail(value);
                break;
        }
        if(userDao.updateUser(user,score)){
            return 1;
        }else{
            return 0;
        }

    }
}
