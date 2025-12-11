package com.financial.sync.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.financial.sync.dto.MessageResponse;
import com.financial.sync.dto.SyncResponseDTO;
import com.financial.sync.dto.XeroAccountDTO;
import com.financial.sync.dto.XeroInvoiceDTO;
import com.financial.sync.dto.XeroTransactionDTO;
import com.financial.sync.entity.User;
import com.financial.sync.entity.XeroStateMapping;
import com.financial.sync.repository.XeroStateMappingRepository;
import com.financial.sync.service.AuthService;
import com.financial.sync.service.UserService;
import com.financial.sync.service.XeroService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/xero")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:3000" })
public class XeroController {

	private final XeroService xeroService;
	private final AuthService authService;
	private final UserService userService;
	private final XeroStateMappingRepository xeroStateMappingRepository;

	@GetMapping("/auth")
	public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
		// User is authenticated here (JWT required for /auth)
		User currentUser = authService.getCurrentUser();

		// Generate random state to tie callback to this user
		String state = UUID.randomUUID().toString();

		XeroStateMapping mapping = new XeroStateMapping();
		mapping.setState(state);
		mapping.setUserId(currentUser.getId());
		xeroStateMappingRepository.save(mapping);

		// Build Xero authorization URL with state
		String authUrl = xeroService.getAuthorizationUrl(state);

		return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
	}

	// ======================
	// 2) XERO CALLBACK
	// ======================
	// Xero calls this AFTER user authorizes.
	// This request is ANONYMOUS (no JWT), so we use `state` to find the user.
	@GetMapping("/callback")
	public ResponseEntity<MessageResponse> handleCallback(@RequestParam String code, @RequestParam String state) {
		try {
			XeroStateMapping mapping = xeroStateMappingRepository.findById(state)
					.orElseThrow(() -> new RuntimeException("Invalid state"));

			Long userId = mapping.getUserId();

			Map<String, String> tokens = xeroService.exchangeCodeForToken(code);

			// 🔥 fetch real tenantId
			String tenantId = xeroService.fetchTenantId(tokens.get("access_token"));

			userService.updateXeroTokens(userId, tokens.get("access_token"), tokens.get("refresh_token"), tenantId);

			xeroStateMappingRepository.deleteById(state);

			return ResponseEntity.ok(new MessageResponse("Xero authentication successful"));
		} catch (Exception e) {
			log.error("Error handling Xero callback: {}", e.getMessage(), e);
			return ResponseEntity.badRequest()
					.body(new MessageResponse("Xero authentication failed: " + e.getMessage()));
		}
	}

	// ======================
	// 3) SYNC + FETCH APIs
	// ======================

	@PostMapping("/invoices/sync")
	public ResponseEntity<SyncResponseDTO> syncInvoices() {
		User currentUser = authService.getCurrentUser();
		SyncResponseDTO response = xeroService.syncInvoices(currentUser);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/accounts/sync")
	public ResponseEntity<SyncResponseDTO> syncAccounts() {
		User currentUser = authService.getCurrentUser();
		SyncResponseDTO response = xeroService.syncAccounts(currentUser);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/transactions/sync")
	public ResponseEntity<SyncResponseDTO> syncTransactions() {
		User currentUser = authService.getCurrentUser();
		SyncResponseDTO response = xeroService.syncTransactions(currentUser);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/invoices")
	public ResponseEntity<List<XeroInvoiceDTO>> getInvoices() {
		User currentUser = authService.getCurrentUser();
		List<XeroInvoiceDTO> invoices = xeroService.getInvoices(currentUser);
		return ResponseEntity.ok(invoices);
	}

	@GetMapping("/transactions")
	public ResponseEntity<List<XeroTransactionDTO>> getTransactions() {
		User currentUser = authService.getCurrentUser();
		List<XeroTransactionDTO> transactions = xeroService.getTransactions(currentUser);
		return ResponseEntity.ok(transactions);
	}

	@GetMapping("/accounts")
	public ResponseEntity<List<XeroAccountDTO>> getAccounts() {
		User currentUser = authService.getCurrentUser();
		List<XeroAccountDTO> accounts = xeroService.getAccounts(currentUser);
		return ResponseEntity.ok(accounts);
	}

	@PostMapping("/refresh-token")
	public ResponseEntity<MessageResponse> refreshToken() {
		try {
			User currentUser = authService.getCurrentUser();
			xeroService.refreshAccessToken(currentUser);
			return ResponseEntity.ok(new MessageResponse("Token refreshed successfully"));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new MessageResponse("Failed to refresh token: " + e.getMessage()));
		}
	}
}
