package com.tanadelgigante.civilservant;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tanadelgigante.civilservant.ServiceConfig.BackendBlock;
import com.tanadelgigante.civilservant.ServiceConfig.HealthBlock;
import com.tanadelgigante.civilservant.ServiceConfig.RouteBlock;
import com.tanadelgigante.civilservant.ServiceConfig.RuntimeBlock;
import com.tanadelgigante.civilservant.ServiceConfig.SetupBlock;
import com.tanadelgigante.civilservant.ServiceConfig.StartBlock;
import com.tanadelgigante.civilservant.ServiceConfig.StopBlock;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class ServiceConfigHelper {

	private static final Logger logger = LoggerFactory.getLogger(ServiceConfigHelper.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final ServiceConfig config;

	private ServiceConfigHelper(ServiceConfig config) {
		this.config = config;
	}

	public static ServiceConfigHelper load(File configFile) throws IOException {
		JsonNode root = MAPPER.readTree(configFile);
		String fileName = configFile.getName();
		if (root == null || !root.isObject()) {
			throw new ServiceConfigException(
					"Configurazione non valida (la radice non e' un oggetto JSON) nel file " + fileName);
		}

		int schemaVersion = root.path("schemaVersion").asInt(1);
		if (schemaVersion < 2) {
			logger.warn("{}: schemaVersion assente o < 2 ({}); interpretato come contratto legacy v1", fileName,
					schemaVersion);
		}

		String name = textOrNull(root, "name");
		if (name == null || name.isBlank()) {
			throw new ServiceConfigException("Campo obbligatorio 'name' mancante o vuoto nel file " + fileName);
		}

		String version = textOrNull(root, "version");
		if (version == null || version.isBlank()) {
			version = "0.0.0-unknown";
			logger.warn("{}: campo 'version' assente; applicato default '{}'", fileName, version);
		}

		String description = textOrNull(root, "description");
		if (description == null || description.isBlank()) {
			description = name;
		}

		String languageField = textOrNull(root, "language");
		if (languageField != null && !languageField.isBlank() && !hasObject(root, "runtime")) {
			logger.warn("{}: campo legacy 'language' usato; in v2 preferire runtime.type", fileName);
		}

		RuntimeBlock runtime = parseRuntime(root, languageField);
		RouteBlock route = parseRoute(root, name, fileName);
		BackendBlock backend = parseBackend(root);
		HealthBlock health = parseHealth(root);
		SetupBlock setup = parseSetup(root);
		StartBlock start = parseStart(root, fileName);
		StopBlock stop = parseStop(root);

		ServiceConfig config = new ServiceConfig(schemaVersion, name, version, description, resolveLanguage(runtime, languageField),
				runtime, route, backend, health, setup, start, stop, configFile.toPath());
		return new ServiceConfigHelper(config);
	}

	public ServiceConfig getConfig() {
		return config;
	}

	public String getName() {
		return config.name();
	}

	public String getLanguage() {
		return config.language();
	}

	public String getRoute() {
		return config.route().prefix();
	}

	public String getStartCommand() {
		return config.start().command();
	}

	private static RuntimeBlock parseRuntime(JsonNode root, String languageField) {
		JsonNode node = root.get("runtime");
		if (node == null || !node.isObject()) {
			String type = (languageField != null && !languageField.isBlank()) ? languageField : "unknown";
			return new RuntimeBlock(type, null);
		}
		String type = textOrNull(node, "type");
		if (type == null || type.isBlank()) {
			type = (languageField != null && !languageField.isBlank()) ? languageField : "unknown";
		}
		String version = textOrNull(node, "version");
		return new RuntimeBlock(type, (version == null || version.isBlank()) ? null : version);
	}

	private static String resolveLanguage(RuntimeBlock runtime, String languageField) {
		if (runtime != null && runtime.type() != null && !runtime.type().isBlank()) {
			return runtime.type();
		}
		if (languageField != null && !languageField.isBlank()) {
			return languageField;
		}
		return "unknown";
	}

	private static RouteBlock parseRoute(JsonNode root, String name, String fileName) {
		JsonNode node = root.get("route");
		if (node != null && node.isTextual()) {
			logger.warn("{}: campo legacy 'route' come stringa; interpretato come {{\"prefix\":\"{}\",\"stripPrefix\":1}}",
					fileName, node.asText());
			return new RouteBlock(normalizePrefix(node.asText()), 1);
		}
		if (node != null && node.isObject()) {
			String prefix = textOrNull(node, "prefix");
			if (prefix == null || prefix.isBlank()) {
				prefix = "/" + name;
			}
			int stripPrefix = node.path("stripPrefix").asInt(1);
			return new RouteBlock(normalizePrefix(prefix), stripPrefix);
		}
		logger.warn("{}: blocco 'route' assente; applicato default '/{}'", fileName, name);
		return new RouteBlock("/" + name, 1);
	}

	private static BackendBlock parseBackend(JsonNode root) {
		JsonNode node = root.get("backend");
		String host = "127.0.0.1";
		Integer port = null;
		String portMode = null;
		if (node != null && node.isObject()) {
			String hostField = textOrNull(node, "host");
			if (hostField != null && !hostField.isBlank()) {
				host = hostField;
			}
			port = integerOrNull(node, "port");
			portMode = textOrNull(node, "portMode");
		}
		if (portMode == null || portMode.isBlank()) {
			portMode = (port != null) ? "fixed" : "auto";
		}
		return new BackendBlock(host, port, portMode);
	}

	private static HealthBlock parseHealth(JsonNode root) {
		JsonNode node = root.get("health");
		String type = "process";
		String path = null;
		int intervalSeconds = 10;
		int timeoutSeconds = 3;
		int startupTimeoutSeconds = 90;
		if (node != null && node.isObject()) {
			String typeField = textOrNull(node, "type");
			if (typeField != null && !typeField.isBlank()) {
				type = typeField;
			}
			path = textOrNull(node, "path");
			intervalSeconds = node.path("intervalSeconds").asInt(10);
			timeoutSeconds = node.path("timeoutSeconds").asInt(3);
			startupTimeoutSeconds = node.path("startupTimeoutSeconds").asInt(90);
		}
		return new HealthBlock(type, path, intervalSeconds, timeoutSeconds, startupTimeoutSeconds);
	}

	private static SetupBlock parseSetup(JsonNode root) {
		JsonNode node = root.get("setup");
		String script = null;
		int timeoutSeconds = 300;
		if (node != null && node.isObject()) {
			script = textOrNull(node, "script");
			timeoutSeconds = node.path("timeoutSeconds").asInt(300);
		}
		return new SetupBlock(script, timeoutSeconds);
	}

	private static StartBlock parseStart(JsonNode root, String fileName) {
		JsonNode startNode = root.get("start");
		String structured = null;
		Map<String, String> env = new LinkedHashMap<>();
		String envFile = null;
		if (startNode != null && startNode.isObject()) {
			structured = textOrNull(startNode, "command");
			JsonNode envNode = startNode.get("env");
			if (envNode != null && envNode.isObject()) {
				for (Map.Entry<String, JsonNode> entry : envNode.properties()) {
					env.put(entry.getKey(), entry.getValue().asText());
				}
			}
			envFile = textOrNull(startNode, "envFile");
		}

		String camel = textOrNull(root, "startCommand");
		String snake = textOrNull(root, "start_command");

		// Ordine di precedenza (R105): start.command > startCommand > start_command.
		Map<String, String> candidates = new LinkedHashMap<>();
		putIfPresent(candidates, "start.command", structured);
		putIfPresent(candidates, "startCommand", camel);
		putIfPresent(candidates, "start_command", snake);

		if (snake != null && !snake.isBlank()) {
			logger.warn("{}: campo legacy 'start_command' usato; in v2 preferire start.command/startCommand", fileName);
		}

		if (candidates.isEmpty()) {
			throw new ServiceConfigException(
					"Campo di avvio obbligatorio mancante ('start.command', 'startCommand' o 'start_command') nel file "
							+ fileName);
		}

		String source = candidates.keySet().iterator().next();
		String command = candidates.get(source);

		// Discordanza fra campi: WARN con il file, tutti i valori presenti e il
		// vincitore; mai un fallimento (R105). La divergenza resta nel blocco e
		// sara' esposta dalla dashboard (R107).
		Map<String, String> divergentFields = new LinkedHashMap<>();
		candidates.forEach((field, value) -> {
			if (!value.equals(command)) {
				divergentFields.put(field, value);
			}
		});
		if (!divergentFields.isEmpty()) {
			logger.warn(
					"{}: campi di avvio discordanti {}; vince '{}' = '{}' (precedenza start.command > startCommand > start_command)",
					fileName, candidates, source, command);
		}

		return new StartBlock(command, env, envFile, source, divergentFields);
	}

	private static void putIfPresent(Map<String, String> target, String key, String value) {
		if (value != null && !value.isBlank()) {
			target.put(key, value);
		}
	}

	private static StopBlock parseStop(JsonNode root) {
		JsonNode node = root.get("stop");
		int timeoutSeconds = 20;
		if (node != null && node.isObject()) {
			timeoutSeconds = node.path("timeoutSeconds").asInt(20);
		}
		return new StopBlock(timeoutSeconds);
	}

	private static String normalizePrefix(String raw) {
		String prefix = raw.trim();
		if (prefix.isEmpty()) {
			return "/";
		}
		if (!prefix.startsWith("/")) {
			prefix = "/" + prefix;
		}
		while (prefix.length() > 1 && prefix.endsWith("/")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		return prefix;
	}

	private static boolean hasObject(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value != null && value.isObject();
	}

	private static String textOrNull(JsonNode node, String field) {
		if (!node.hasNonNull(field)) {
			return null;
		}
		return node.get(field).asText();
	}

	private static Integer integerOrNull(JsonNode node, String field) {
		if (!node.hasNonNull(field)) {
			return null;
		}
		return node.get(field).asInt();
	}
}
