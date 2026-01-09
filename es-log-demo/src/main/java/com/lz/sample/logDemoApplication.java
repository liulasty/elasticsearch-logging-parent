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

    @Bean(destroyMethod = "close")
    public SimpleEsWriter simpleEsWriter(
            @Value("${es.host:localhost}") String host,
            @Value("${es.port:9200}") int port,
            @Value("${es.username:}") String username,
            @Value("${es.password:}") String password
    ) {
        String normalizedUsername = (username == null || username.trim().isEmpty()) ? null : username;
        String normalizedPassword = (password == null || password.trim().isEmpty()) ? null : password;
        return new SimpleEsWriter(host, port, normalizedUsername, normalizedPassword);
    }

    @Bean
    public ApplicationRunner runner() {
        return args -> args.getNonOptionArgs().forEach(arg -> log.info("arg: {}", arg));
    }
}

