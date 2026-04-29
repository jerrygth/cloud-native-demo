package com.org.dev.cloud.useraccount;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserAccountRegistrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserAccountRegistrationApplication.class, args);
	}

}
