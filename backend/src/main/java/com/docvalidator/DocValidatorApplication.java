package com.docvalidator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DocValidator - AI-Powered API Documentation Testing Framework
 * 
 * Main application class that bootstraps the Spring Boot application.
 * This framework validates API documentation against live systems using
 * AI agents and Model Context Protocol (MCP).
 * 
 * @author DocValidator Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DocValidatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocValidatorApplication.class, args);
        
        System.out.println("""
            
            ╔═══════════════════════════════════════════════════════════╗
            ║                                                           ║
            ║              DocValidator Started Successfully            ║
            ║                                                           ║
            ║   AI-Powered API Documentation Testing Framework         ║
            ║                                                           ║
            ║   Dashboard: http://localhost:8080                        ║
            ║   API Docs:  http://localhost:8080/swagger-ui            ║
            ║   H2 Console: http://localhost:8080/h2-console           ║
            ║                                                           ║
            ╚═══════════════════════════════════════════════════════════╝
            
            """);
    }
}

// Made with Bob
