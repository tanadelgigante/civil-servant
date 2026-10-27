package com.tanadelgigante.civilservant;

import java.nio.file.Path;
import java.util.Map;

public record ServiceConfig(
		int schemaVersion,
		String name,
		String version,
		String description,
		String language,
		RuntimeBlock runtime,
		RouteBlock route,
		BackendBlock backend,
		HealthBlock health,
		SetupBlock setup,
		StartBlock start,
		StopBlock stop,
		Path sourceFile) {

	public record RuntimeBlock(String type, String version) {
	}

	public record RouteBlock(String prefix, int stripPrefix) {
	}

	public record BackendBlock(String host, Integer port, String portMode) {
	}

	public record HealthBlock(String type, String path, int intervalSeconds, int timeoutSeconds, int startupTimeoutSeconds) {
	}

	public record SetupBlock(String script, int timeoutSeconds) {
	}

	/**
	 * Blocco di avvio risolto.
	 *
	 * @param command         comando vincente (mai vuoto)
	 * @param env             variabili d'ambiente dedicate al plugin
	 * @param envFile         file d'ambiente opzionale (puo' essere null)
	 * @param source          nome del campo da cui proviene {@code command}
	 *                        ({@code start.command}, {@code startCommand} o
	 *                        {@code start_command}); usato dalla dashboard per
	 *                        rendere visibile la precedenza applicata (R105)
	 * @param divergentFields campi di avvio presenti ma con valore diverso dal
	 *                        comando vincente (mappa nome-campo → valore); vuota
	 *                        quando non c'e' discordanza. La dashboard la espone
	 *                        quando non vuota (R107)
	 */
	public record StartBlock(String command, Map<String, String> env, String envFile, String source,
			Map<String, String> divergentFields) {
	}

	public record StopBlock(int timeoutSeconds) {
	}
}
