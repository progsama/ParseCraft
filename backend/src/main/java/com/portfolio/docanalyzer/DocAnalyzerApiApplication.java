package com.portfolio.docanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocAnalyzerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocAnalyzerApiApplication.class, args);
    }
}
