package com.finansal.finansal_deneme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling

@SpringBootApplication
public class FinansalApplication {

	public static void main(String[] args) {
		//IPv4 öncelikli ayarları (bazı ağ sorunları için)
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");
		
		// DNS önbellek ayarları (network sorunları için)
		System.setProperty("networkaddress.cache.ttl", "60");
		System.setProperty("networkaddress.cache.negative.ttl", "10");
		
		
		
		SpringApplication.run(FinansalApplication.class, args);
	}

}
