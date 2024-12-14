package com.tanadelgigante.civilservant.security;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class FlexibleAuthenticationFilter implements GlobalFilter, Ordered {
	private static final Logger logger = LoggerFactory.getLogger(FlexibleAuthenticationFilter.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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
			// Load service-specific authentication configuration
			ServiceAuthConfig authConfig = loadServiceAuthConfig(servicePath);

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

	private ServiceAuthConfig loadServiceAuthConfig(String servicePath) {
		try {
			// Locate service-config.json for the specific service
			File configFile = new File("services" + servicePath, "service-config.json");
			if (!configFile.exists()) {
				logger.warn("No service-config.json found for path: {}", servicePath);
				return null;
			}

			JsonNode config = objectMapper.readTree(configFile);

			// Extract authentication configuration
			JsonNode authNode = config.path("auth");
			if (authNode.isMissingNode()) {
				logger.info("No auth configuration found in service-config.json for {}", servicePath);
				return null;
			}

			return new ServiceAuthConfig(authNode.path("type").asText("token"),
					authNode.path("validation_strategy").asText("exact_match"),
					authNode.path("expected_token").asText(), authNode.path("token_regex").asText(""));
		} catch (IOException e) {
			logger.error("Error reading service configuration", e);
			return null;
		}
	}

	private boolean validateTokenAgainstConfig(String token, ServiceAuthConfig config) {
		switch (config.validationStrategy) {
		case "exact_match":
			return token.equals(config.expectedToken);
		case "prefix":
			return token.startsWith(config.expectedToken);
		case "regex":
			return Pattern.matches(config.tokenRegex, token);
		case "jwt":
			return validateJwtToken(token);
		default:
			logger.warn("Unknown validation strategy: {}", config.validationStrategy);
			return false;
		}
	}

	private boolean validateJwtToken(String token) {
		// Placeholder for JWT validation
		// In a real implementation, use a JWT library like JJWT
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

	@Override
	public int getOrder() {
		return -100; // High priority, run early in filter chain
	}

	// Inner classes for configuration and token extraction
	private interface TokenExtractionMethod {
		String extractToken(ServerWebExchange exchange);
	}

	private static class ServiceAuthConfig {
		final String type;
		final String validationStrategy;
		final String expectedToken;
		final String tokenRegex;

		ServiceAuthConfig(String type, String validationStrategy, String expectedToken, String tokenRegex) {
			this.type = type;
			this.validationStrategy = validationStrategy;
			this.expectedToken = expectedToken;
			this.tokenRegex = tokenRegex;
		}
	}
}