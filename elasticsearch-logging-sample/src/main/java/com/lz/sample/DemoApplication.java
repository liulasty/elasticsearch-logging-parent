package com.lz.sample;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public ApplicationRunner runner(ApplicationContext ctx) {
        return args -> {
            System.out.println(">>>> ApplicationRunner executed <<<<");
            
            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            
            System.out.println("====== Loaded Beans (" + beanNames.length + ") ======");
            for (String beanName : beanNames) {
                if (beanName.contains("Controller") || beanName.contains("Appender")) {
                    System.out.println(beanName + " : " + ctx.getBean(beanName).getClass().getName());
                }
            }
            System.out.println("============================================");
        };
    }
}
