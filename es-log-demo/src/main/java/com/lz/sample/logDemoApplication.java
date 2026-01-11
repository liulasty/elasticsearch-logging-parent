package com.lz.sample;

import com.lz.sample.es.SimpleEsWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class logDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(logDemoApplication.class, args);
    }

    @Bean
    public ApplicationRunner runner() {
        return args -> args.getNonOptionArgs().forEach(arg -> log.info("arg: {}", arg));
    }
}

