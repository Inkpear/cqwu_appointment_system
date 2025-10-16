package com.cqwu.cqwu_appointment_system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@MapperScan("com.cqwu.cqwu_appointment_system.mapper")
public class CqwuAppointmentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CqwuAppointmentSystemApplication.class, args);
    }

}
