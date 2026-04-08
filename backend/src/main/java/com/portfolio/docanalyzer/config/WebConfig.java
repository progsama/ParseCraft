package com.portfolio.docanalyzer.config;

import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        List<String> origins = Objects.requireNonNullElse(corsProperties.allowedOrigins(), List.of());
        registry.addMapping("/api/**")
                .allowedOrigins(StringUtils.toStringArray(origins))
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
