package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        // This is the entry point of the Spring Boot application.
        // SpringApplication.run() starts the embedded server and configures the application context.
        SpringApplication.run(App.class, args);
    }
}