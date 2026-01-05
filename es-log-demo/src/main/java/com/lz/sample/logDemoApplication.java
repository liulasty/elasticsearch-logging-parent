package com.lz.sample;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class logDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(logDemoApplication.class, args);
    }
    @Bean
    public ApplicationRunner runner() {
        return args -> {
            args.getNonOptionArgs().forEach(System.out::println);
            System.out.println(">>>> ApplicationRunner executed <<<<");
            // 打印所有加载好的bean

        };
    }

}
