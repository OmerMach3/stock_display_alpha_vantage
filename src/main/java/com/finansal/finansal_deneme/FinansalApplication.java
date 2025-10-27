package com.finansal.finansal_deneme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan // Automatically picks up @ConfigurationProperties beans (e.g., FinancialApiProperties)
@EnableCaching // Enables IoC-managed caching via annotations like @Cacheable and @CacheEvict
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
