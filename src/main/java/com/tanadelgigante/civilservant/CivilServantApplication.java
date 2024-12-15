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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class CivilServantApplication {

	private static final Logger logger = LoggerFactory.getLogger(CivilServantApplication.class);

	// Registry for tracking registered services
	private static final ConcurrentHashMap<String, ServiceDescriptor> servicesRegistry = new ConcurrentHashMap<>();
	private static final Set<String> existingRoutes = new HashSet<>();

	public static void main(String[] args) {
		SpringApplication.run(CivilServantApplication.class, args);
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
		File configFile = servicePath.resolve("service-config.json").toFile();
		ServiceConfigHelper serviceHelper = ServiceConfigHelper.getInstance();
		serviceHelper.loadConfig(configFile);

		String name = serviceHelper.getName();
		String language = serviceHelper.getLanguage();
		String basePath = servicePath.toString();
		String startCommand = serviceHelper.getStartCommand();

		ServiceDescriptor descriptor = new ServiceDescriptor(name, language, basePath, startCommand);

		startService(descriptor);
		servicesRegistry.put(descriptor.name, descriptor);
		logger.info("Service {} registered successfully.", descriptor.name);
	}

	private static void startService(ServiceDescriptor descriptor) {
	    try {
	        File setupScript = new File(descriptor.basePath, "setup.sh");
	        if (!setupScript.exists()) {
	            throw new RuntimeException("Missing setup.sh in " + descriptor.basePath);
	        }

	        ProcessBuilder processBuilder = new ProcessBuilder("bash", setupScript.getAbsolutePath());
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
		JsonNode envVariables = ServiceConfigHelper.getInstance().getEnvironment();
		envVariables.fields().forEachRemaining(entry -> {
			String key = entry.getKey();
			String value = entry.getValue().asText();
			processBuilder.environment().put(key, value);
		});
	}

	private static void configureVolumes(ServiceDescriptor descriptor) {
		JsonNode volumes = ServiceConfigHelper.getInstance().getVolumes();
		volumes.forEach(volume -> {
			String source = volume.path("source").asText();
			String target = volume.path("target").asText();
			boolean readOnly = volume.path("read_only").asBoolean();
			logger.info("Configuring volume for service {}: source={}, target={}, readOnly={}", descriptor.name, source,
					target, readOnly);
			// Questo è un esempio di logging, l'effettiva implementazione della gestione
			// dei volumi dipenderà dai tuoi requisiti specifici.
		});
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		servicesRegistry.values().forEach(descriptor -> {
			String route = getRouteForService(descriptor);
			routes.route(descriptor.name,
					r -> r.path(route + "/**").filters(f -> f.stripPrefix(1)).uri("http://localhost:8187"));
			logger.info("Configured route {} for service {}", route, descriptor.name);
		});

		return routes.build();
	}

	private static String getRouteForService(ServiceDescriptor descriptor) {
		String route = ServiceConfigHelper.getInstance().getRoute();

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

@RestController
class TestController {
	@GetMapping("/test")
	public Mono<String> test() {
		return Mono.just("Civil Servant Gateway is working!");
	}
}
