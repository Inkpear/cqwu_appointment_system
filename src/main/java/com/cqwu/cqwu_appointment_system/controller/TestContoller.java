package com.cqwu.cqwu_appointment_system.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestContoller {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot...";
    }

}
