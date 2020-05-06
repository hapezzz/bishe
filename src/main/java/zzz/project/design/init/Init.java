package zzz.project.design.init;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import zzz.project.design.jobs.CountNum;
import zzz.project.design.jobs.RewriteAof;

@Component
@Order(1)
public class Init implements CommandLineRunner {

    @Autowired
    private Scheduler scheduler;

    @Override
    public void run(String... args) throws Exception {
        JobBuilder rewriteBuilder = JobBuilder.newJob(RewriteAof.class);
        JobBuilder countBuilder = JobBuilder.newJob(CountNum.class);

        JobDetail rewriteDetail = rewriteBuilder.withIdentity("rewrite","group").build();
        JobDetail countDetail = countBuilder.withIdentity("count","group").build();

        TriggerBuilder<Trigger> rewritetriggerBuilder = TriggerBuilder.newTrigger().withIdentity("rewrite","group");
        TriggerBuilder<Trigger> counttriggerBuilder = TriggerBuilder.newTrigger().withIdentity("count","group");
        rewritetriggerBuilder.startNow();
        counttriggerBuilder.startNow();

        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("* * 8 ? * * *");
        countBuilder.withIdentity("jobs","group");

        CronTrigger rewriteTrigger = rewritetriggerBuilder.withSchedule(cronScheduleBuilder).build();
        CronTrigger countTrigger = counttriggerBuilder.withSchedule(cronScheduleBuilder).build();

        scheduler.scheduleJob(rewriteDetail,rewriteTrigger);
        scheduler.scheduleJob(countDetail,countTrigger);
    }
}
