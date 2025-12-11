package com.financial.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.sync.dto.SyncResponseDTO;
import com.financial.sync.dto.XeroAccountDTO;
import com.financial.sync.dto.XeroInvoiceDTO;
import com.financial.sync.dto.XeroTransactionDTO;
import com.financial.sync.entity.*;
import com.financial.sync.exception.XeroAuthException;
import com.financial.sync.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class XeroServiceImpl implements XeroService {

	private final XeroInvoiceRepository invoiceRepository;
	private final XeroAccountRepository accountRepository;
	private final XeroTransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final SyncLogService syncLogService;
	private final WebClient xeroWebClient;
	private final ObjectMapper objectMapper;

	@Value("${xero.client-id}")
	private String clientId;

	@Value("${xero.client-secret}")
	private String clientSecret;

	@Value("${xero.redirect-uri}")
	private String redirectUri;

	@Value("${xero.scope}")
	private String scope;

	@Value("${xero.authorization-url}")
	private String authorizationUrl;

	@Value("${xero.token-url}")
	private String tokenUrl;

	@Value("${xero.api-url}")
	private String apiUrl;

	@Override
	public String getAuthorizationUrl(String state) {
		String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
		String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);

		return authorizationUrl + "?response_type=code" + "&client_id=" + clientId + "&redirect_uri=" + encodedRedirect
				+ "&scope=" + encodedScope + "&state=" + state;
	}

	@Override
	public Map<String, String> exchangeCodeForToken(String code) {
		try {
			String response = xeroWebClient.post().uri(tokenUrl)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
					.body(BodyInserters.fromFormData("grant_type", "authorization_code").with("client_id", clientId)
							.with("client_secret", clientSecret).with("code", code).with("redirect_uri", redirectUri))
					.retrieve().bodyToMono(String.class).block();

			JsonNode jsonNode = objectMapper.readTree(response);

			Map<String, String> tokens = new HashMap<>();
			tokens.put("access_token", jsonNode.get("access_token").asText());
			tokens.put("refresh_token", jsonNode.get("refresh_token").asText());

			return tokens;

		} catch (Exception e) {
			log.error("Error exchanging code for token", e);
			throw new XeroAuthException("Failed to exchange code for token");
		}
	}

	@Override
	@Transactional
	public String fetchTenantId(String accessToken) {
		try {
			String response = xeroWebClient.get().uri("https://api.xero.com/connections")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).retrieve().bodyToMono(String.class)
					.block();

			JsonNode list = objectMapper.readTree(response);

			if (list.isArray() && list.size() > 0) {
				return list.get(0).get("tenantId").asText();
			}

			throw new XeroAuthException("No Xero connections found");

		} catch (Exception e) {
			log.error("Error fetching Xero tenant ID", e);
			throw new XeroAuthException("Failed to fetch Xero tenant ID");
		}
	}

	@Override
	@Transactional
	public SyncResponseDTO syncInvoices(User user) {
		SyncLog syncLog = syncLogService.startSync(user, "INVOICE");

		try {
			validateAuth(user);

			String response = xeroWebClient.get().uri(apiUrl + "/Invoices")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.getXeroAccessToken())
					.header("Xero-Tenant-Id", user.getXeroTenantId()).retrieve().bodyToMono(String.class).block();

			JsonNode invoicesNode = objectMapper.readTree(response).get("Invoices");
			int syncedCount = 0;

			if (invoicesNode != null && invoicesNode.isArray()) {
				for (JsonNode node : invoicesNode) {
					XeroInvoice invoice = parseInvoice(node, user);

					Optional<XeroInvoice> existing = invoiceRepository.findByXeroInvoiceId(invoice.getXeroInvoiceId());
					existing.ifPresent(value -> invoice.setId(value.getId()));

					invoiceRepository.save(invoice);
					syncedCount++;
				}
			}

			syncLogService.completeSync(syncLog, syncedCount, "SUCCESS", null);
			return new SyncResponseDTO("SUCCESS", "Invoices synced successfully");

		} catch (Exception e) {
			log.error("Error syncing invoices", e);
			syncLogService.completeSync(syncLog, 0, "FAILED", e.getMessage());
			return new SyncResponseDTO("FAILED", e.getMessage());
		}
	}

	@Override
	@Transactional
	public SyncResponseDTO syncAccounts(User user) {
		SyncLog syncLog = syncLogService.startSync(user, "ACCOUNT");

		try {
			validateAuth(user);

			String response = xeroWebClient.get().uri(apiUrl + "/Accounts")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.getXeroAccessToken())
					.header("Xero-Tenant-Id", user.getXeroTenantId()).retrieve().bodyToMono(String.class).block();

			JsonNode accountsNode = objectMapper.readTree(response).get("Accounts");
			int syncedCount = 0;

			if (accountsNode != null && accountsNode.isArray()) {
				for (JsonNode node : accountsNode) {
					XeroAccount account = parseAccount(node, user);

					Optional<XeroAccount> existing = accountRepository.findByXeroAccountId(account.getXeroAccountId());
					existing.ifPresent(value -> account.setId(value.getId()));

					accountRepository.save(account);
					syncedCount++;
				}
			}

			syncLogService.completeSync(syncLog, syncedCount, "SUCCESS", null);
			return new SyncResponseDTO("SUCCESS", "Accounts synced successfully");

		} catch (Exception e) {
			log.error("Error syncing accounts", e);
			syncLogService.completeSync(syncLog, 0, "FAILED", e.getMessage());
			return new SyncResponseDTO("FAILED", e.getMessage());
		}
	}

	@Override
	@Transactional
	public SyncResponseDTO syncTransactions(User user) {
		SyncLog syncLog = syncLogService.startSync(user, "TRANSACTION");

		try {
			validateAuth(user);

			String response = xeroWebClient.get().uri(apiUrl + "/BankTransactions")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.getXeroAccessToken())
					.header("Xero-Tenant-Id", user.getXeroTenantId()).retrieve().bodyToMono(String.class).block();

			JsonNode transactionsNode = objectMapper.readTree(response).get("BankTransactions");
			int syncedCount = 0;

			if (transactionsNode != null && transactionsNode.isArray()) {
				for (JsonNode node : transactionsNode) {
					XeroTransaction transaction = parseTransaction(node, user);

					Optional<XeroTransaction> existing = transactionRepository
							.findByXeroTransactionId(transaction.getXeroTransactionId());
					existing.ifPresent(value -> transaction.setId(value.getId()));

					transactionRepository.save(transaction);
					syncedCount++;
				}
			}

			syncLogService.completeSync(syncLog, syncedCount, "SUCCESS", null);
			return new SyncResponseDTO("SUCCESS", "Transactions synced successfully");

		} catch (Exception e) {
			log.error("Error syncing transactions", e);
			syncLogService.completeSync(syncLog, 0, "FAILED", e.getMessage());
			return new SyncResponseDTO("FAILED", e.getMessage());
		}
	}

	private void validateAuth(User user) {
		if (user.getXeroAccessToken() == null) {
			throw new XeroAuthException("User not authenticated with Xero");
		}

		if (isTokenExpired(user)) {
			refreshAccessToken(user);
		}
	}

	private boolean isTokenExpired(User user) {
		return user.getTokenExpiry() == null || LocalDateTime.now().isAfter(user.getTokenExpiry().minusMinutes(5));
	}

	@Transactional
	public void saveTokensAndTenant(User user, String accessToken, String refreshToken) {
		user.setXeroAccessToken(accessToken);
		user.setXeroRefreshToken(refreshToken);
		user.setTokenExpiry(LocalDateTime.now().plusMinutes(25));

		String tenantId = fetchTenantId(accessToken);
		user.setXeroTenantId(tenantId);

		userRepository.save(user);
	}

	private XeroInvoice parseInvoice(JsonNode node, User user) {
		XeroInvoice invoice = new XeroInvoice();
		invoice.setXeroInvoiceId(node.get("InvoiceID").asText());
		invoice.setInvoiceNumber(node.path("InvoiceNumber").asText(null));
		invoice.setContactName(node.path("Contact").path("Name").asText(null));
		invoice.setStatus(node.path("Status").asText(null));
		invoice.setType(node.path("Type").asText(null));
		invoice.setSubTotal(new BigDecimal(node.path("SubTotal").asText("0")));
		invoice.setTotalTax(new BigDecimal(node.path("TotalTax").asText("0")));
		invoice.setTotal(new BigDecimal(node.path("Total").asText("0")));
		invoice.setAmountDue(new BigDecimal(node.path("AmountDue").asText("0")));
		invoice.setAmountPaid(new BigDecimal(node.path("AmountPaid").asText("0")));
		invoice.setCurrencyCode(node.path("CurrencyCode").asText("USD"));

		if (node.has("Date")) {
			invoice.setInvoiceDate(parseXeroDate(node.path("Date").asText(null)));
		}
		if (node.has("DueDate")) {
			invoice.setDueDate(parseXeroDate(node.path("DueDate").asText(null)));
		}

		invoice.setUser(user);
		return invoice;
	}

	private XeroAccount parseAccount(JsonNode node, User user) {
		XeroAccount account = new XeroAccount();
		account.setXeroAccountId(node.get("AccountID").asText());
		account.setAccountCode(node.path("Code").asText(null));
		account.setAccountName(node.path("Name").asText(null));
		account.setAccountType(node.path("Type").asText(null));
		account.setAccountClass(node.path("Class").asText(null));
		account.setStatus(node.path("Status").asText(null));
		account.setDescription(node.path("Description").asText(null));
		account.setTaxType(node.path("TaxType").asText(null));
		account.setEnablePaymentsToAccount(node.path("EnablePaymentsToAccount").asBoolean(false));
		account.setUser(user);
		return account;
	}

	private XeroTransaction parseTransaction(JsonNode node, User user) {
		XeroTransaction transaction = new XeroTransaction();
		transaction.setXeroTransactionId(node.get("BankTransactionID").asText());
		transaction.setTransactionType(node.path("Type").asText(null));
		transaction.setContactName(node.path("Contact").path("Name").asText(null));
		transaction.setStatus(node.path("Status").asText(null));
		transaction.setReference(node.path("Reference").asText(null));

		if (node.has("Date")) {
			transaction.setTransactionDate(parseXeroDate(node.path("Date").asText(null)));
		}

		transaction.setAmount(new BigDecimal(node.path("Total").asText("0")));
		transaction.setUser(user);
		return transaction;
	}

	@Override
	public List<XeroInvoiceDTO> getInvoices(User user) {
		return invoiceRepository.findByUser(user).stream().map(this::convertToInvoiceDTO).collect(Collectors.toList());
	}

	private XeroInvoiceDTO convertToInvoiceDTO(XeroInvoice invoice) {
		XeroInvoiceDTO dto = new XeroInvoiceDTO();
		dto.setId(invoice.getId());
		dto.setInvoiceNumber(invoice.getInvoiceNumber());
		dto.setContactName(invoice.getContactName());
		dto.setInvoiceDate(invoice.getInvoiceDate());
		dto.setDueDate(invoice.getDueDate());
		dto.setStatus(invoice.getStatus());
		dto.setTotal(invoice.getTotal());
		dto.setAmountDue(invoice.getAmountDue());
		return dto;
	}

	@Override
	public List<XeroAccountDTO> getAccounts(User user) {
		return accountRepository.findByUser(user).stream().map(this::convertToAccountDTO).collect(Collectors.toList());
	}

	private XeroAccountDTO convertToAccountDTO(XeroAccount account) {
		XeroAccountDTO dto = new XeroAccountDTO();
		dto.setId(account.getId());
		dto.setAccountCode(account.getAccountCode());
		dto.setAccountName(account.getAccountName());
		dto.setAccountType(account.getAccountType());
		dto.setStatus(account.getStatus());
		return dto;
	}

	@Override
	@Transactional
	public void refreshAccessToken(User user) {
		try {
			String response = xeroWebClient.post().uri(tokenUrl)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
					.body(BodyInserters.fromFormData("grant_type", "refresh_token").with("client_id", clientId)
							.with("client_secret", clientSecret).with("refresh_token", user.getXeroRefreshToken()))
					.retrieve().bodyToMono(String.class).block();

			JsonNode jsonNode = objectMapper.readTree(response);

			String newAccessToken = jsonNode.get("access_token").asText();
			String newRefreshToken = jsonNode.get("refresh_token").asText();

			user.setXeroAccessToken(newAccessToken);
			user.setXeroRefreshToken(newRefreshToken);
			user.setTokenExpiry(LocalDateTime.now().plusMinutes(25));

			userRepository.save(user);

			log.info("Xero token refreshed successfully for user {}", user.getId());

		} catch (Exception e) {
			log.error("Error refreshing access token: {}", e.getMessage(), e);
			throw new XeroAuthException("Failed to refresh access token");
		}
	}

	private LocalDate parseXeroDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		value = value.trim();

		// Microsoft JSON format: /Date(1672531200000+0000)/
		if (value.startsWith("/Date(")) {
			try {
				// Extract number between /Date( and + or )/
				int start = value.indexOf("(") + 1;
				int end = value.indexOf("+") > 0 ? value.indexOf("+") : value.indexOf(")");
				long millis = Long.parseLong(value.substring(start, end));
				return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
			} catch (Exception e) {
				log.warn("Failed to parse Microsoft JSON date: {}", value);
				return null;
			}
		}

		// ISO 8601 format
		try {
			return LocalDate.parse(value.substring(0, 10));
		} catch (Exception ignored) {
		}

		log.warn("Unknown date format: {}", value);
		return null;
	}

	@Override
	public List<XeroTransactionDTO> getTransactions(User user) {
		return transactionRepository.findByUser(user).stream().map(this::convertToTransactionDTO)
				.collect(Collectors.toList());
	}

	private XeroTransactionDTO convertToTransactionDTO(XeroTransaction transaction) {
		XeroTransactionDTO dto = new XeroTransactionDTO();
		dto.setId(transaction.getId());
		dto.setTransactionType(transaction.getTransactionType());
		dto.setContactName(transaction.getContactName());
		dto.setTransactionDate(transaction.getTransactionDate());
		dto.setAmount(transaction.getAmount());
		dto.setAccountCode(transaction.getAccountCode());
		dto.setAccountName(transaction.getAccountName());
		dto.setDescription(transaction.getDescription());
		dto.setReference(transaction.getReference());
		dto.setStatus(transaction.getStatus());
		return dto;
	}

}
