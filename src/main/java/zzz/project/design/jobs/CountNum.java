package zzz.project.design.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CountNum implements Job {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        long numoflogin = redisTemplate.opsForHash().size("numoflogin");
        long numofadd = redisTemplate.opsForHash().size("numofadd");
        redisTemplate.delete("numoflogin");
        redisTemplate.delete("numofadd");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR,-1);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String value = simpleDateFormat.format(calendar.getTime());
        redisTemplate.opsForHash().put("countoflogin",value,numoflogin);
        redisTemplate.opsForHash().put("countofadd",value,numofadd);
        logger.debug("start");
    }
}
