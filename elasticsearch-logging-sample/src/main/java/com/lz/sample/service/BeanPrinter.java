package com.lz.sample.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class BeanPrinter implements ApplicationRunner {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Arrays.sort(beanNames);

        System.out.println("====== Spring 加载的 Bean 列表 ======");
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            System.out.println(beanName + " -> " + bean.getClass().getName());
        }
        System.out.println("====== 共 " + beanNames.length + " 个 Bean ======");
    }
}
