package com.tanadelgigante.civilservant;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

	static final ConcurrentHashMap<String, ServiceDescriptor> servicesRegistry = new ConcurrentHashMap<>();

	// Route gia' assegnate: route pubblica -> plugin che la possiede. La mappa
	// consente al WARN di conflitto di citare il plugin in conflitto (Task 3.7).
	private final Map<String, String> existingRoutes = new HashMap<>();

	// Directory di discovery dei bundle plugin, ancorata a una property esplicita
	// per non dipendere dalla working directory (Task 3.8). Default di produzione:
	// /app/services (nella working directory del container, via compose).
	@Value("${civilservant.services-dir:/app/services}")
	private String servicesDirectory;

	public static void main(String[] args) {
		SpringApplication.run(CivilServantApplication.class, args);
		logger.info("Civil Servant Application started on port 8187");
	}

	static class ServiceDescriptor {
		final String name;
		final String language;
		final String basePath;
		final ServiceConfig config;
		Process process;
		// Route pubblicata assegnata (puo' differire da config.route().prefix() in
		// caso di conflitto) e plugin con cui l'eventuale conflitto e' avvenuto.
		String assignedRoute;
		String routeConflictOwner;

		ServiceDescriptor(ServiceConfig config, String basePath) {
			this.name = config.name();
			this.language = config.language();
			this.basePath = basePath;
			this.config = config;
		}
	}

	private void discoverAndRegisterServices() {
		File servicesDir = new File(servicesDirectory);
		if (!servicesDir.exists() || !servicesDir.isDirectory()) {
			logger.warn("Services directory not found: {}", servicesDirectory);
			return;
		}

		try (var serviceStream = Files.walk(servicesDir.toPath())) {
			List<Path> servicePaths = serviceStream.filter(Files::isDirectory)
					.filter(path -> new File(path.toFile(), "service-config.json").exists())
					.collect(Collectors.toList());

			for (Path servicePath : servicePaths) {
				try {
					registerService(servicePath);
				} catch (Exception e) {
					logger.error("Failed to register service at {}: {}", servicePath, e.getMessage(), e);
				}
			}
		} catch (IOException e) {
			logger.error("Error scanning services directory.", e);
		}
	}

	private static void registerService(Path servicePath) throws IOException {
		File configFile = servicePath.resolve("service-config.json").toFile();
		ServiceConfigHelper serviceHelper = ServiceConfigHelper.load(configFile);
		ServiceConfig config = serviceHelper.getConfig();
		logger.info("Service {} found (schemaVersion {})", config.name(), config.schemaVersion());

		ServiceDescriptor descriptor = new ServiceDescriptor(config, servicePath.toString());
		startService(descriptor);
		servicesRegistry.put(descriptor.name, descriptor);
		logger.info("Service {} registered successfully.", descriptor.name);
	}

	private static void startService(ServiceDescriptor descriptor) {
		try {
			runSetupIfConfigured(descriptor);

			ProcessBuilder startProcessBuilder = new ProcessBuilder("bash", "-c", descriptor.config.start().command());
			startProcessBuilder.directory(new File(descriptor.basePath));
			descriptor.process = startProcessBuilder.start();
			logger.info("Started service {} with start command", descriptor.name);
			captureOutput(descriptor.process, descriptor.name);
		} catch (IOException | InterruptedException e) {
			logger.error("Failed to start service {}: {}", descriptor.name, e.getMessage(), e);
			throw new RuntimeException("Failed to start service: " + descriptor.name, e);
		}
	}

	private static void runSetupIfConfigured(ServiceDescriptor descriptor) throws IOException, InterruptedException {
		String script = descriptor.config.setup().script();
		if (script == null || script.isBlank()) {
			File legacySetup = new File(descriptor.basePath, "setup.sh");
			if (!legacySetup.exists()) {
				return;
			}
			script = "setup.sh";
		}

		File setupScript = new File(descriptor.basePath, script);
		if (!setupScript.exists()) {
			logger.warn("Setup script '{}' configured but not found for service {}; setup skipped", script,
					descriptor.name);
			return;
		}

		ProcessBuilder setupProcessBuilder = new ProcessBuilder("bash", setupScript.getAbsolutePath());
		setupProcessBuilder.directory(new File(descriptor.basePath));
		Process setupProcess = setupProcessBuilder.start();
		captureOutput(setupProcess, descriptor.name);
		setupProcess.waitFor();
		logger.info("Setup completed for service {}", descriptor.name);
	}

	private static void captureOutput(Process process, String serviceName) {
		new Thread(() -> {
			try {
				process.getInputStream().transferTo(System.out);
			} catch (IOException e) {
				logger.error("Error capturing output for service {}", serviceName, e);
			}
		}).start();
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		// Discovery DENTRO la costruzione del contesto: quando il bean viene
		// interrogato, il registry e' gia' popolato (Task 3.6, difetto 2).
		discoverAndRegisterServices();

		RouteLocatorBuilder.Builder routes = builder.routes();

		servicesRegistry.values().forEach(descriptor -> {
			String uri = resolveBackendUri(descriptor);
			if (uri == null) {
				logger.warn(
						"Service {}: nessuna porta backend dichiarata (portMode '{}'); route non pubblicata finche' la porta non viene allocata (FASE 5)",
						descriptor.name, descriptor.config.backend().portMode());
				return;
			}

			String route = getRouteForService(descriptor);
			int stripPrefix = descriptor.config.route().stripPrefix();
			routes.route(descriptor.name,
					r -> r.path(route + "/**").filters(f -> f.stripPrefix(stripPrefix)).uri(uri));
			logger.info("Configured route {} -> {} for service {}", route, uri, descriptor.name);
		});

		return routes.build();
	}

	/**
	 * Risolve l'URI di backend a cui la route del plugin inoltra. L'URI deriva dal
	 * blocco {@code backend} (host/port), mai dalla porta del gateway stesso
	 * (difetto 1). Se la porta non e' dichiarata (portMode {@code auto}) restituisce
	 * {@code null}: l'allocazione e' di competenza della FASE 5.
	 */
	private static String resolveBackendUri(ServiceDescriptor descriptor) {
		ServiceConfig.BackendBlock backend = descriptor.config.backend();
		if (backend.port() != null) {
			return "http://" + backend.host() + ":" + backend.port();
		}
		return null;
	}

	private String getRouteForService(ServiceDescriptor descriptor) {
		String route = descriptor.config.route().prefix();

		if (existingRoutes.containsKey(route)) {
			String owner = existingRoutes.get(route);
			String uniqueRoute;
			int counter = 1;
			do {
				uniqueRoute = route + "-" + counter++;
			} while (existingRoutes.containsKey(uniqueRoute));
			existingRoutes.put(uniqueRoute, descriptor.name);
			descriptor.assignedRoute = uniqueRoute;
			descriptor.routeConflictOwner = owner;
			logger.warn(
					"Route conflict detected: plugin '{}' vuole '{}', gia' usata da '{}'; assigned unique route: {}",
					descriptor.name, route, owner, uniqueRoute);
			return uniqueRoute;
		}

		existingRoutes.put(route, descriptor.name);
		descriptor.assignedRoute = route;
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
