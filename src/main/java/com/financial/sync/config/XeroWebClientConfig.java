package com.financial.sync.config;

import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import reactor.netty.http.client.HttpClient;

@Configuration
public class XeroWebClientConfig {

	@Bean
	 WebClient xeroWebClient(WebClient.Builder builder) throws Exception {

		// Java SSLContext for TLS 1.2
		SSLContext jdkContext = SSLContext.getInstance("TLSv1.2");
		jdkContext.init(null, null, null);

		// Convert to Netty SSLContext
		SslContext nettySslContext = new JdkSslContext(jdkContext, true, // isClient
				ClientAuth.NONE); // no client certificate

		HttpClient httpClient = HttpClient.create().secure(ssl -> ssl.sslContext(nettySslContext))
				.responseTimeout(Duration.ofSeconds(60));

		return builder.clientConnector(new ReactorClientHttpConnector(httpClient))
				.defaultHeader("Accept", "application/json").build();
	}
}