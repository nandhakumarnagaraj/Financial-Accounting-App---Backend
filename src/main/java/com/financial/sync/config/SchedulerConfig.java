package com.financial.sync.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
	// Scheduling is already enabled in main application class
	// This config class is for additional scheduler configurations if needed

}
