package com.sleekydz86.kopanda.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.disable() }
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/**").permitAll()
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/test/**").permitAll()
                    .anyRequest().permitAll()
            }

        return http.build()
    }
}