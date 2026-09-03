package com.abnamro.assignment;

import com.abnamro.assignment.helper.RestAssuredWrapper;
import com.abnamro.assignment.helper.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class BaseSetup {

    protected final Logger log =
            LoggerFactory.getLogger(getClass());

    protected final String projectId;
    protected final String token;
    protected final RestAssuredWrapper restAssuredWrapper;

    protected BaseSetup() {
        String environment = System.getProperty("env", "default").trim();

        if (!environment.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Invalid environment name: " + environment
            );
        }

        String configPath = "default".equals(environment)
                ? "configs/config.yaml"
                : "configs/config-" + environment + ".yaml";

        Map<String, Object> config = Utilities.readYaml(configPath);

        Object gitlabSection = config.get("gitlab");

        if (!(gitlabSection instanceof Map<?, ?> gitlabConfig)) {
            throw new IllegalArgumentException(
                    "Missing or invalid 'gitlab' section in " + configPath
            );
        }

        String baseUrl = requiredValue(gitlabConfig, "base-url");
        projectId = requiredValue(gitlabConfig, "project-id");

        String tokenVariable = requiredValue(
                gitlabConfig,
                "token-environment-variable"
        );

        token = System.getenv(tokenVariable);

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Required environment variable is not set: " + tokenVariable
            );
        }

        restAssuredWrapper = new RestAssuredWrapper(baseUrl);

        log.info("Initialized API setup for environment: {}", environment);
    }

    private static String requiredValue(Map<?, ?> config, String key) {
        Object value = config.get(key);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing configuration value: gitlab." + key
            );
        }

        return value.toString();
    }
}