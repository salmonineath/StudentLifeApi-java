package com.studentlife.studentlifejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class StudentlifejavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentlifejavaApplication.class, args);
		System.out.println("Application is running!");
	}

}
