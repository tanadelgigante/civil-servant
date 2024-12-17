package com.tanadelgigante.civilservant;

import java.io.File;
import java.io.IOException;

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

	public String getStartCommand() {
		return config.path("start_command").asText();
	}
}
