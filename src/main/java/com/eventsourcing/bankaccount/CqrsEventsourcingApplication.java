package com.eventsourcing.bankaccount;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CqrsEventsourcingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CqrsEventsourcingApplication.class, args);
	}

}
