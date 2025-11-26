package com.newwork.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EmployeeProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeProfileApplication.class, args);
    }
}
