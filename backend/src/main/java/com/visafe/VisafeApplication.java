package com.visafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VisafeApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisafeApplication.class, args);
    }
}
