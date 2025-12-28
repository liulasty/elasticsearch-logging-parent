package com.lz.sample.scheduler;

import com.lz.sample.service.LogStorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class LogStorageScheduler {

    @Autowired
    private LogStorageService logStorageService;

    // 每分钟执行一次日志存储任务
    @Scheduled(fixedRate = 10000) // 60秒
    public void storeLogsPeriodically() {
        logStorageService.storeLogsToFile();
    }

    // 或者按固定时间执行，例如每天凌晨2点
    @Scheduled(cron = "0 0 2 * * ?")
    public void storeLogsDaily() {
        logStorageService.storeLogsToFile();
    }
}

