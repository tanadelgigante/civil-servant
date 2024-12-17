package com.tanadelgigante.civilservant;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServiceConfigHelper {
	private JsonNode config;

	private static ServiceConfigHelper instance;

	private ServiceConfigHelper() {
	}

	public static synchronized ServiceConfigHelper getInstance() {
		if (instance == null) {
			instance = new ServiceConfigHelper();
		}
		return instance;
	}

	public void loadConfig(File configFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		this.config = objectMapper.readTree(configFile);
	}

	public JsonNode getConfig() {
		return config;
	}

	public String getName() {
		return config.path("name").asText();
	}

	public String getLanguage() {
		return config.path("language").asText();
	}

	public String getRoute() {
		return config.path("route").asText();
	}

	public JsonNode getAuth() {
		return config.path("auth");
	}

	public String getAuthToken() {
		return config.path("auth_token").asText();
	}

	public String getStartCommand() {
		return config.path("start_command").asText();
	}

	public JsonNode getEnvironment() {
		return config.path("environment");
	}

	public JsonNode getVolumes() {
		return config.path("volumes");
	}

	public List<String> getSupportedExtractionMethods() {
		JsonNode methodsNode = config.path("auth").path("supported_extraction_methods");
		if (methodsNode.isArray()) {
			return new ObjectMapper().convertValue(methodsNode, List.class);
		}
		return List.of();
	}

	public String getValidationStrategy() {
	    // Use path() with a default value to handle different key naming
	    return config.path("auth").path("validationStrategy").asText(config.path("auth").path("validation_strategy").asText());
	}

	public String getExpectedToken() {
		return config.path("auth").path("expected_token").asText();
	}

	public String getTokenRegex() {
		return config.path("auth").path("token_regex").asText();
	}
}
