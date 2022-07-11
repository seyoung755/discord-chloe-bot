package com.example.discordbotspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.security.auth.login.LoginException;

@SpringBootApplication
public class DiscordBotSpringApplication {

	public static void main(String[] args) throws LoginException {
		SpringApplication.run(DiscordBotSpringApplication.class, args);

		MessageListener.run();
	}

}
