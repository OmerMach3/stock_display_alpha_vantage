package com.finansal.finansal_deneme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS setup so the Angular app (localhost:4200) can call the backend API (localhost:8080).
        registry.addMapping("/api/**") // Only allow paths starting with /api/.
            .allowedOrigins("http://localhost:4200") // Only accept requests from this origin.
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // HTTP methods we allow.
            .allowedHeaders("*") // Allow all headers.
            .allowCredentials(true); // Allow sending credentials (cookies, etc.).
    }
}
