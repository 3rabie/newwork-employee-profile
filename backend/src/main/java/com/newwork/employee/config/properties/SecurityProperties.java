package com.newwork.employee.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Externalized security configuration (CORS + demo toggles).
 */
@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private final Cors cors = new Cors();
    private final Demo demo = new Demo();

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = true;
    }

    @Data
    public static class Demo {
        private boolean switchUserEnabled = false;
    }
}
