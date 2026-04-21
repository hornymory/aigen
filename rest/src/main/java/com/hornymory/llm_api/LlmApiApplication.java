package com.hornymory.llm_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class LlmApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmApiApplication.class, args);
	}

}
