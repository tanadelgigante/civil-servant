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

import reactor.core.publisher.Mono;

@SpringBootApplication
public class CivilServantApplication {

	private static final Logger logger = LoggerFactory.getLogger(CivilServantApplication.class);

	// Registry for tracking registered services
	static final ConcurrentHashMap<String, ServiceDescriptor> servicesRegistry = new ConcurrentHashMap<>();
	private static final Set<String> existingRoutes = new HashSet<>();

	public static void main(String[] args) {
		SpringApplication.run(CivilServantApplication.class, args);
		logger.info("Civil Servant Application started on port 8187");
		discoverAndRegisterServices();
		logger.info("Services loaded!");
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
		logger.info("Service {} found", name);
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

			ProcessBuilder setupProcessBuilder = new ProcessBuilder("bash", setupScript.getAbsolutePath());
			setupProcessBuilder.directory(new File(descriptor.basePath));

			Process setupProcess = setupProcessBuilder.start();
			// Capture and log the output
			new Thread(() -> {
				try {
					setupProcess.getInputStream().transferTo(System.out);
				} catch (IOException e) {
					logger.error("Error capturing output for service {}", descriptor.name, e);
				}
			}).start();
			setupProcess.waitFor(); // Attende il completamento dello script di setup

			logger.info("Setup completed for service {}", descriptor.name);

			// Lancia il comando di avvio
			if (descriptor.startCommand != null && !descriptor.startCommand.isEmpty()) {
				ProcessBuilder startProcessBuilder = new ProcessBuilder("bash", "-c", descriptor.startCommand);
				startProcessBuilder.directory(new File(descriptor.basePath));

				descriptor.process = startProcessBuilder.start();

				logger.info("Started service {} with start command", descriptor.name);

				// Capture and log the output
				new Thread(() -> {
					try {
						descriptor.process.getInputStream().transferTo(System.out);
					} catch (IOException e) {
						logger.error("Error capturing output for service {}", descriptor.name, e);
					}
				}).start();
			}
		} catch (IOException | InterruptedException e) {
			logger.error("Failed to start service {}: {}", descriptor.name, e.getMessage(), e);
			throw new RuntimeException("Failed to start service: " + descriptor.name, e);
		}
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
	private static final Logger logger = LoggerFactory.getLogger(TestController.class);

	@GetMapping("/test")
	public Mono<String> test() {
		logger.info("Test endpoint called");
		return Mono.just("Civil Servant Gateway is working!");
	}
}