package zzz.project.design.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RewriteAof implements Job {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String command = "/redis/redis-5.0.7/src/redis-cli -p 6379 -h 127.0.0.1 BGREWRITEAOF";

        try{
            logger.debug("start");
            Runtime.getRuntime().exec(command);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
