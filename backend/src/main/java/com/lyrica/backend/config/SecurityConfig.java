package com.lyrica.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("http://localhost:3000"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                return config;
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**", "/uploads/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(absolutePath);
    }
}