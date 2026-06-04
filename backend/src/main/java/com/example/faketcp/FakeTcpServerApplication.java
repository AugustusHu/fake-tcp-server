package com.example.faketcp;

import com.example.faketcp.config.FakeTcpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FakeTcpProperties.class)
public class FakeTcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FakeTcpServerApplication.class, args);
    }
}
