package com.kartik.foodglance;

import com.kartik.foodglance.filter.RateLimitFilter;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FoodGlanceApplication {

	public static void main(String[] args) {
		// Load .env file for local development.
		// On cloud platforms (Railway, Render), env vars are injected by the platform
		// and .env won't exist — ignoreIfMissing() ensures the app still starts.
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));

		SpringApplication.run(FoodGlanceApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
		FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new RateLimitFilter());
		registration.addUrlPatterns("/detect-food");
		return registration;
	}
}
