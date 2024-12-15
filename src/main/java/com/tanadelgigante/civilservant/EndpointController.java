package com.tanadelgigante.civilservant;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class EndpointController {

	@GetMapping("/endpoints")
	public Mono<Set<String>> getEndpoints() {
		Set<String> endpoints = CivilServantApplication.servicesRegistry.keySet();
		return Mono.just(endpoints);
	}
}
