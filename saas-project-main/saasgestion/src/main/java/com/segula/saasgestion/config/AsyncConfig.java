package com.segula.saasgestion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Active @Async sur EmailService pour que les envois d'email
    // ne bloquent pas la réponse HTTP
}