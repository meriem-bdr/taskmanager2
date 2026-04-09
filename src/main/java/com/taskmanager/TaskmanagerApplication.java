package com.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  // <-- must be at root package
public class TaskmanagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskmanagerApplication.class, args);
    }
}