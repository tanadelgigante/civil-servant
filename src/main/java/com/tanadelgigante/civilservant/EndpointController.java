package com.tanadelgigante.civilservant;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class EndpointController {
	private static final Logger logger = LoggerFactory.getLogger(EndpointController.class);

	@GetMapping("/endpoints")
	public Mono<Set<String>> getEndpoints() {
		Set<String> endpoints = CivilServantApplication.servicesRegistry.keySet();
		logger.info("Endpoints list endpoint called");
		return Mono.just(endpoints);
	}
}
