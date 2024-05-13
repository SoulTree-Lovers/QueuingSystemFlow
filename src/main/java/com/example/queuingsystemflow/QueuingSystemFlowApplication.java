package com.example.queuingsystemflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QueuingSystemFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueuingSystemFlowApplication.class, args);
	}

}
