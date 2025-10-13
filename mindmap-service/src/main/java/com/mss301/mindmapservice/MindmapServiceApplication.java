package com.mss301.mindmapservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MindmapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindmapServiceApplication.class, args);
    }
}
