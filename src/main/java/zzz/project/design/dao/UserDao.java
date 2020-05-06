package zzz.project.design.dao;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import zzz.project.design.entity.User;

import java.util.Set;

@Slf4j
@Repository
public class UserDao {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public boolean uniqueEmail(String email) {
        if (redisTemplate.opsForHash().hasKey("emails", email)) {
            return false;
        }
        return true;
    }

    public boolean uniqueAccount(String account) {
        if (redisTemplate.opsForHash().hasKey("accounts", account)) {
            return false;
        }
        return true;
    }

    public double getScore() {//最大的分数加1
        long count = redisTemplate.opsForZSet().size("users");
        if (count < 1) {
            return 1;
        }
        count--;
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().rangeWithScores("users", count, count);
        double score = 1;
        for (ZSetOperations.TypedTuple<Object> typedTuple : set) {
            score = typedTuple.getScore();
        }
        score++;
        return score;
    }

    public String getName(String account) {
        double score = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForHash().get("accounts", account)), Double.class);
        Set<Object> set = redisTemplate.opsForZSet().rangeByScore("users", score, score);
        if (set != null) {
            User user = null;
            for (Object temp : set) {
                user = JSONObject.parseObject(JSONObject.toJSONString(temp), User.class);
            }
            return user.getUser_name();
        } else {
            return "";
        }
    }

    public User getUser(String account) {
        double score = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForHash().get("accounts", account)), Double.class);
        Set<Object> set = redisTemplate.opsForZSet().rangeByScore("users", score, score);
        if (set != null) {
            User user = null;
            for (Object temp : set) {
                user = JSONObject.parseObject(JSONObject.toJSONString(temp), User.class);
            }
            return user;
        } else {
            return null;
        }
    }

    public User EgetUser(String email){
        if(!redisTemplate.opsForHash().hasKey("emails",email)){
            return null;
        }
        double score = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForHash().get("emails", email)), Double.class);
        Set<Object> set = redisTemplate.opsForZSet().rangeByScore("users", score, score);
        if (set != null) {
            User user = null;
            for (Object temp : set) {
                user = JSONObject.parseObject(JSONObject.toJSONString(temp), User.class);
            }
            return user;
        } else {
            return null;
        }
    }

    public User getTempUser(String account) {
        double score = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForHash().get("accounts", account)), Double.class);
        Set<Object> set = redisTemplate.opsForZSet().rangeByScore("tempUsers", score, score);
        if (set != null) {
            User user = null;
            for (Object temp : set) {
                user = JSONObject.parseObject(JSONObject.toJSONString(temp), User.class);
            }
            return user;
        } else {
            return null;
        }
    }

    public boolean deleteUser(String account) {
        User user = getUser(account);
        String email = user.getEmail();
        try {
            redisTemplate.opsForZSet().remove("users", user);
            redisTemplate.opsForHash().delete("accounts", account);
            redisTemplate.opsForHash().delete("emails", email);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(User user,double score) {
        try{

            redisTemplate.opsForZSet().removeRangeByScore("users",score,score);
            redisTemplate.opsForZSet().add("users",user,score);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
