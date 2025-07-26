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
            .authorizeHttpRequests { authz ->
                authz
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/api-docs.yaml",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/swagger-resources/**"
                    ).permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/**").permitAll()
                    .anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions().disable()
            }

        return http.build()
    }
}