package com.docvalidator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for DocValidator application.
 * Maps to docvalidator.* properties in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "docvalidator")
@Data
public class DocValidatorConfig {

    private TargetApi targetApi = new TargetApi();
    private Authentication authentication = new Authentication();
    private Ai ai = new Ai();
    private TestGeneration testGeneration = new TestGeneration();
    private Validation validation = new Validation();
    private Mcp mcp = new Mcp();
    private Reporting reporting = new Reporting();

    @Data
    public static class TargetApi {
        private String name;
        private String baseUrl;
        private String openapiSpecUrl;
        private Integer timeout = 30000;
        private Integer maxRetries = 3;
    }

    @Data
    public static class Authentication {
        private String type;
        private Spotify spotify = new Spotify();

        @Data
        public static class Spotify {
            private String clientId;
            private String clientSecret;
            private String tokenUrl;
            private List<String> scopes;
        }
    }

    @Data
    public static class Ai {
        private String provider;
        private OpenAi openai = new OpenAi();
        private Claude claude = new Claude();

        @Data
        public static class OpenAi {
            private String apiKey;
            private String model;
            private Double temperature;
            private Integer maxTokens;
        }

        @Data
        public static class Claude {
            private String apiKey;
            private String model;
            private Double temperature;
            private Integer maxTokens;
        }
    }

    @Data
    public static class TestGeneration {
        private Boolean enabled = true;
        private Boolean generateEdgeCases = true;
        private Boolean generateNegativeTests = true;
        private Boolean generateWorkflowTests = true;
        private Integer maxTestsPerEndpoint = 10;
    }

    @Data
    public static class Validation {
        private Boolean strictMode = false;
        private Boolean semanticAnalysis = true;
        private Boolean schemaValidation = true;
        private Integer responseTimeThreshold = 5000;
        private Boolean failOnDiscrepancy = false;
    }

    @Data
    public static class Mcp {
        private Boolean enabled = true;
        private McpServer docServer = new McpServer();
        private McpServer apiServer = new McpServer();

        @Data
        public static class McpServer {
            private Boolean enabled = true;
            private Integer port;
        }
    }

    @Data
    public static class Reporting {
        private String outputDir = "target/reports";
        private List<String> formats;
        private Boolean includeRecommendations = true;
        private List<String> severityLevels;
    }
}

// Made with Bob
