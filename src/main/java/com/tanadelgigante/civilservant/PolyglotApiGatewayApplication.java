package com.tanadelgigante.civilservant;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PolyglotApiGatewayApplication {

	private static final Logger logger = LoggerFactory.getLogger(PolyglotApiGatewayApplication.class);

	// Registry for tracking registered services
	private static final ConcurrentHashMap<String, ServiceDescriptor> servicesRegistry = new ConcurrentHashMap<>();
	private static final Set<String> existingRoutes = new HashSet<>();

	public static void main(String[] args) {
		SpringApplication.run(PolyglotApiGatewayApplication.class, args);
		discoverAndRegisterServices();
	}

	// Service Descriptor structure
	static class ServiceDescriptor {
		String name;
		String language;
		String basePath;
		String startCommand;
		Process process;

		ServiceDescriptor(String name, String language, String basePath, String startCommand) {
			this.name = name;
			this.language = language;
			this.basePath = basePath;
			this.startCommand = startCommand;
		}
	}

	private static void discoverAndRegisterServices() {
		File servicesDirectory = new File("services");
		if (!servicesDirectory.exists() || !servicesDirectory.isDirectory()) {
			logger.warn("Services directory not found.");
			return;
		}

		try {
			// Recursively scan the services directory
			List<Path> servicePaths = Files.walk(servicesDirectory.toPath()).filter(Files::isDirectory)
					.filter(path -> new File(path.toFile(), "service-config.json").exists())
					.collect(Collectors.toList());

			for (Path servicePath : servicePaths) {
				try {
					registerService(servicePath);
				} catch (Exception e) {
					logger.error("Failed to register service at {}", servicePath, e);
				}
			}
		} catch (IOException e) {
			logger.error("Error scanning services directory.", e);
		}
	}

	private static void registerService(Path servicePath) throws IOException {
		String configContent = Files.readString(servicePath.resolve("service-config.json"));

		// Parse the configuration (placeholder - replace with proper JSON parsing)
		ServiceDescriptor descriptor = new ServiceDescriptor("example-service", "python", servicePath.toString(),
				determineStartCommand(servicePath));

		startService(descriptor);
		servicesRegistry.put(descriptor.name, descriptor);
		logger.info("Service {} registered successfully.", descriptor.name);
	}

	private static String determineStartCommand(Path servicePath) {
		if (new File(servicePath.toFile(), "app.py").exists()) {
			return "python app.py";
		}
		if (new File(servicePath.toFile(), "main.js").exists()) {
			return "node main.js";
		}
		if (new File(servicePath.toFile(), "main.go").exists()) {
			return "go run main.go";
		}
		if (new File(servicePath.toFile(), "Cargo.toml").exists()) {
			return "cargo run";
		}
		if (new File(servicePath.toFile(), "main.java").exists()) {
			return "javac Main.java && java Main";
		}
		if (new File(servicePath.toFile(), "index.rb").exists()) {
			return "ruby index.rb";
		}
		throw new RuntimeException("Unsupported language in service at " + servicePath);
	}

	private static void startService(ServiceDescriptor descriptor) {
		try {
			File setupScript = new File(descriptor.basePath, "setup.sh");
			if (!setupScript.exists() || !setupScript.canExecute()) {
				throw new RuntimeException("Missing or non-executable setup.sh in " + descriptor.basePath);
			}

			ProcessBuilder processBuilder = new ProcessBuilder("./setup.sh");
			processBuilder.directory(new File(descriptor.basePath));

			// Configura le variabili d'ambiente
			configureEnvironment(processBuilder, descriptor);

			// Configura i volumi
			configureVolumes(descriptor);

			descriptor.process = processBuilder.start();

			logger.info("Started service {} with setup.sh", descriptor.name);

			// Capture and log the output
			new Thread(() -> {
				try {
					descriptor.process.getInputStream().transferTo(System.out);
				} catch (IOException e) {
					logger.error("Error capturing output for service {}", descriptor.name, e);
				}
			}).start();
		} catch (IOException e) {
			logger.error("Failed to start service {}: {}", descriptor.name, e.getMessage(), e);
			throw new RuntimeException("Failed to start service: " + descriptor.name, e);
		}
	}

	private static void configureEnvironment(ProcessBuilder processBuilder, ServiceDescriptor descriptor) {
		// Load environment variables from service-config.json
		File configFile = new File(descriptor.basePath, "service-config.json");
		try {
			String configContent = Files.readString(configFile.toPath());
			if (configContent.contains("\"environment\"")) {
				// Simple JSON parsing (use Jackson/Gson for production)
				int start = configContent.indexOf("\"environment\":") + 14;
				int end = configContent.indexOf("}", start);
				String envJson = configContent.substring(start, end + 1);

				// Parse and set environment variables
				String[] envPairs = envJson.replace("{", "").replace("}", "").split(",");
				for (String pair : envPairs) {
					String[] keyValue = pair.split(":");
					String key = keyValue[0].trim().replace("\"", "");
					String value = keyValue[1].trim().replace("\"", "");
					processBuilder.environment().put(key, value);
				}
			}
		} catch (IOException e) {
			logger.error("Error reading environment variables for service {}", descriptor.name, e);
		}
	}

	private static void configureVolumes(ServiceDescriptor descriptor) {
		File configFile = new File(descriptor.basePath, "service-config.json");
		try {
			String configContent = Files.readString(configFile.toPath());
			if (configContent.contains("\"volumes\"")) {
				// Parse and handle volumes
				int start = configContent.indexOf("\"volumes\":") + 10;
				int end = configContent.indexOf("]", start);
				String volumesJson = configContent.substring(start, end + 1);

				// This is a placeholder. Adjust to mount local paths or create directories if
				// needed.
				logger.info("Configuring volumes for service {}: {}", descriptor.name, volumesJson);
			}
		} catch (IOException e) {
			logger.error("Error reading volumes for service {}", descriptor.name, e);
		}
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		servicesRegistry.values().forEach(descriptor -> {
			String route = getRouteForService(descriptor);
			routes.route(descriptor.name,
					r -> r.path(route + "/**").filters(f -> f.stripPrefix(1)).uri("http://localhost:8080")); // Servizi
																												// esposti
																												// sulla
																												// stessa
																												// porta
			logger.info("Configured route {} for service {}", route, descriptor.name);
		});

		return routes.build();
	}

	private static String getRouteForService(ServiceDescriptor descriptor) {
		// Parsing del file di configurazione per ottenere la route
		File configFile = new File(descriptor.basePath, "service-config.json");
		String route;
		try {
			String configContent = Files.readString(configFile.toPath());
			// Semplice parsing JSON (puoi sostituirlo con Jackson o Gson)
			if (configContent.contains("\"route\"")) {
				int start = configContent.indexOf("\"route\":") + 9;
				int end = configContent.indexOf("\"", start + 1);
				route = configContent.substring(start, end).trim();
			} else {
				route = "/" + descriptor.name.replace(" ", "-").toLowerCase();
			}
		} catch (IOException e) {
			logger.error("Error reading route configuration for service {}", descriptor.name, e);
			route = "/" + descriptor.name.replace(" ", "-").toLowerCase();
		}

		// Risoluzione dei conflitti
		if (!existingRoutes.add(route)) {
			String uniqueRoute;
			int counter = 1;
			do {
				uniqueRoute = route + "-" + counter++;
			} while (!existingRoutes.add(uniqueRoute));
			logger.warn("Route conflict detected for {}. Assigned unique route: {}", descriptor.name, uniqueRoute);
			return uniqueRoute;
		}

		return route;
	}
}
