package com.javaSaga.Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class main{

    public static void main(String[] args) {
        SpringApplication.run(main.class, args);
    }
}
