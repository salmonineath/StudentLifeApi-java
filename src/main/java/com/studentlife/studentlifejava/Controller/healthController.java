package com.studentlife.studentlifejava.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class healthController {
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
