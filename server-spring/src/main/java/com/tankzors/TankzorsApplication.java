package com.tankzors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the web, persistence, campaign API and co-op relay server. */
@SpringBootApplication
public class TankzorsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TankzorsApplication.class, args);
    }
}
