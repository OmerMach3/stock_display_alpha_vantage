package com.finansal.finansal_deneme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Angular uygulamasının (localhost:4200) backend API'sine (localhost:8080)
        // erişebilmesi için CORS (Cross-Origin Resource Sharing) ayarları.
        registry.addMapping("/api/**") // Sadece /api/ ile başlayan yollara izin ver.
                .allowedOrigins("http://localhost:4200") // Sadece bu adresten gelen isteklere izin ver.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // İzin verilen HTTP metodları.
                .allowedHeaders("*") // Tüm header'lara izin ver.
                .allowCredentials(true); // Kimlik bilgileri (cookie vb.) gönderimine izin ver.
    }
}
