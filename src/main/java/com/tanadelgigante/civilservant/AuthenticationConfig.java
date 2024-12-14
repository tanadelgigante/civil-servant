package com.tanadelgigante.civilservant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tanadelgigante.civilservant.security.FlexibleAuthenticationFilter;

@Configuration
public class AuthenticationConfig {
	@Bean
	public FlexibleAuthenticationFilter flexibleAuthenticationFilter() {
		return new FlexibleAuthenticationFilter();
	}
}