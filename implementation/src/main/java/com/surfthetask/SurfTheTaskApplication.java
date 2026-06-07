package com.surfthetask;

import com.surfthetask.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SurfTheTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SurfTheTaskApplication.class, args);
    }
}
