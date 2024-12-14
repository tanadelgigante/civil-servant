package com.tanadelgigante.civilservant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class CivilServantApplication extends PolyglotApiGatewayApplication{

	public static void main(String[] args) {
		SpringApplication.run(CivilServantApplication.class, args);
	}
}

@RestController
class TestController {

	@GetMapping("/test")
	public String test() {
		return "Civil Servant Gateway is working!";
	}
}
