package com.tanadelgigante.civilservant;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class FlexibleAuthenticationFilter extends AbstractGatewayFilterFactory<ServiceConfigHelper> {
	private static final Logger logger = LoggerFactory.getLogger(FlexibleAuthenticationFilter.class);

	public FlexibleAuthenticationFilter() {
		super(ServiceConfigHelper.class);
	}

	@Override
	public GatewayFilter apply(ServiceConfigHelper config) {
		return (exchange, chain) -> {
			// Extract token from multiple sources
			String token = extractToken(exchange);

			// Determine the service route
			String servicePath = determineServicePath(exchange);

			// Validate token for the specific service
			if (isTokenValid(token, servicePath)) {
				// Add token to downstream request headers if validation succeeds
				ServerWebExchange modifiedExchange = enhanceExchangeWithToken(exchange, token);
				return chain.filter(modifiedExchange);
			}

			// Unauthorized access
			return unauthorized(exchange);
		};
	}

	private String extractToken(ServerWebExchange exchange) {
		// Bearer Token from Authorization Header
		String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}

		// Custom X-Auth-Token Header
		String customHeader = exchange.getRequest().getHeaders().getFirst("X-Auth-Token");
		if (customHeader != null) {
			return customHeader;
		}

		// Query Parameter
		String queryToken = exchange.getRequest().getQueryParams().getFirst("auth_token");
		if (queryToken != null) {
			return queryToken;
		}

		// Path-based Token
		String path = exchange.getRequest().getPath().toString();
		int tokenIndex = path.indexOf("/token/");
		if (tokenIndex != -1) {
			return path.substring(tokenIndex + 7);
		}

		return null;
	}

	private String determineServicePath(ServerWebExchange exchange) {
		// Extract service path from the request
		String path = exchange.getRequest().getPath().toString();
		String[] pathSegments = path.split("/");
		return pathSegments.length > 1 ? "/" + pathSegments[1] : path;
	}

	private boolean isTokenValid(String token, String servicePath) {
		if (token == null) {
			logger.warn("No token found for service path: {}", servicePath);
			return false;
		}

		try {
			// Load service-specific authentiServicecation configuration
			ServiceConfigHelper authConfig = loadServiceAuthConfig(servicePath);

			if (authConfig == null) {
				logger.warn("No authentication configuration found for service: {}", servicePath);
				return false;
			}

			return validateTokenAgainstConfig(token, authConfig);
		} catch (Exception e) {
			logger.error("Token validation error for service {}: {}", servicePath, e.getMessage());
			return false;
		}
	}

	private ServiceConfigHelper loadServiceAuthConfig(String servicePath) {
		try {
			// Locate service-config.json for the specific service
			File configFile = new File("services" + servicePath, "service-config.json");
			ServiceConfigHelper serviceHelper = ServiceConfigHelper.getInstance();
			serviceHelper.loadConfig(configFile);

			return serviceHelper;
		} catch (IOException e) {
			logger.error("Error reading service configuration", e);
			return null;
		}
	}

	private boolean validateTokenAgainstConfig(String token, ServiceConfigHelper config) {
		switch (config.getValidationStrategy()) {
		case "exact_match":
			return token.equals(config.getExpectedToken());
		case "prefix":
			return token.startsWith(config.getExpectedToken());
		case "regex":
			return Pattern.matches(config.getTokenRegex(), token);
		case "jwt":
			return validateJwtToken(token);
		default:
			logger.warn("Unknown validation strategy: {}", config.getValidationStrategy());
			return false;
		}
	}

	private boolean validateJwtToken(String token) {
		// Placeholder for JWT validation
		try {
			// Validate token structure, signature, expiration, etc.
			return token != null && token.split("\\.").length == 3;
		} catch (Exception e) {
			logger.error("JWT validation failed", e);
			return false;
		}
	}

	private ServerWebExchange enhanceExchangeWithToken(ServerWebExchange exchange, String token) {
		// Add token to downstream request headers for service-level authentication
		return exchange.mutate().request(exchange.getRequest().mutate().header("X-Internal-Auth-Token", token).build())
				.build();
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange) {
		logger.warn("Unauthorized access attempt from IP: {}", exchange.getRequest().getRemoteAddress());
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}
}
