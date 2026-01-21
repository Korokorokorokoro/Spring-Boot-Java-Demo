package com.springbootjavatest.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.springbootjavatest.demo.model.Order;
import com.springbootjavatest.demo.repository.OrderRepository;

// Main application class for Spring Boot demo application.
@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

    // Initialize sample data in the database at startup.
	@Bean
    CommandLineRunner init(OrderRepository repo) {
        return args -> {
            repo.save(new Order("Order 1"));
            repo.save(new Order("Order 2"));
			repo.save(new Order("Order 3"));
            repo.findAll().forEach(order -> 
                System.out.println(order.getId() + " - " + order.getDetails() + " - " + order.getState())
            );
        };
	}
}
