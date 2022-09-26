package com.xyz.gumall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
//@EnableAsync
//@EnableScheduling
@Component
public class HelloSchedule {

//    @Scheduled(cron = "0 0 3 * * ?")
    @Async
    @Scheduled(cron = "* * * ? * 2")
    public void hello() throws InterruptedException {
        log.info("hello...");
        Thread.sleep(3000);
    }
}
