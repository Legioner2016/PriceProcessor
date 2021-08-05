package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.price.processor.PriceProcessor;

@SpringBootApplication(scanBasePackages = {"com.example.demo", "com.price.processor"})
public class PriceProcessorApplication {
	
	@Autowired
	@Qualifier("priceThrottler")
	PriceProcessor  priceThrottler;


	public static void main(String[] args) {
		SpringApplication.run(PriceProcessorApplication.class, args);
	}

}
