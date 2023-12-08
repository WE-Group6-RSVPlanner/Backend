package com.rsvpplaner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories("com.rsvpplaner.repository")
public class JpaConfig {

}
