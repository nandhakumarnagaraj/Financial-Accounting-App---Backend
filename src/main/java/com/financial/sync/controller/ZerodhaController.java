package com.financial.sync.controller;

import com.financial.sync.dto.*;
import com.financial.sync.entity.User;
import com.financial.sync.entity.ZerodhaStateMapping;
import com.financial.sync.repository.ZerodhaStateMappingRepository;
import com.financial.sync.service.AuthService;
import com.financial.sync.service.UserService;
import com.financial.sync.service.ZerodhaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/zerodha")
@RequiredArgsConstructor
public class ZerodhaController {

    private final ZerodhaService zerodhaService;
    private final AuthService authService;
    private final UserService userService;
    private final ZerodhaStateMappingRepository zerodhaStateMappingRepository;

    @Value("${zerodha.api-secret}")
    private String apiSecret;

    @GetMapping("/auth")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        try {
            log.info("Getting Zerodha authorization URL");
            User currentUser = authService.getCurrentUser();

            String state = UUID.randomUUID().toString();

            ZerodhaStateMapping mapping = new ZerodhaStateMapping();
            mapping.setState(state);
            mapping.setUserId(currentUser.getId());
            zerodhaStateMappingRepository.save(mapping);

            String authUrl = zerodhaService.getAuthorizationUrl(state);
            log.info("Zerodha auth URL generated successfully for user: {}", currentUser.getUsername());

            return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
        } catch (Exception e) {
            log.error("Error getting Zerodha auth URL: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate authorization URL: " + e.getMessage()));
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<MessageResponse> handleCallback(
            @RequestParam String request_token,
            @RequestParam String state) {
        try {
            log.info("Handling Zerodha callback with state: {}", state);
            
            ZerodhaStateMapping mapping = zerodhaStateMappingRepository.findById(state)
                    .orElseThrow(() -> new RuntimeException("Invalid state"));

            Long userId = mapping.getUserId();
            log.info("Found user ID: {} for state: {}", userId, state);

            Map<String, String> tokens = zerodhaService.exchangeRequestTokenForAccessToken(request_token, apiSecret);
            log.info("Successfully exchanged request token for access token");

            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setZerodhaAccessToken(tokens.get("access_token"));
            user.setTokenExpiry(LocalDateTime.now().plusDays(1)); // Zerodha tokens are valid for 1 day
            userService.updateUser(user);
            log.info("Zerodha tokens saved for user: {}", user.getUsername());

            zerodhaStateMappingRepository.deleteById(state);

            return ResponseEntity.ok(new MessageResponse("Zerodha authentication successful"));
        } catch (Exception e) {
            log.error("Error handling Zerodha callback: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Zerodha authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/holdings/sync")
    public ResponseEntity<SyncResponseDTO> syncHoldings() {
        try {
            log.info("Starting Zerodha holdings sync");
            User currentUser = authService.getCurrentUser();
            SyncResponseDTO response = zerodhaService.syncHoldings(currentUser);
            log.info("Zerodha holdings sync completed: {}", response.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error syncing holdings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new SyncResponseDTO("FAILED", "Error syncing holdings: " + e.getMessage()));
        }
    }

    @PostMapping("/positions/sync")
    public ResponseEntity<SyncResponseDTO> syncPositions() {
        try {
            log.info("Starting Zerodha positions sync");
            User currentUser = authService.getCurrentUser();
            SyncResponseDTO response = zerodhaService.syncPositions(currentUser);
            log.info("Zerodha positions sync completed: {}", response.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error syncing positions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new SyncResponseDTO("FAILED", "Error syncing positions: " + e.getMessage()));
        }
    }

    @PostMapping("/orders/sync")
    public ResponseEntity<SyncResponseDTO> syncOrders() {
        try {
            log.info("Starting Zerodha orders sync");
            User currentUser = authService.getCurrentUser();
            SyncResponseDTO response = zerodhaService.syncOrders(currentUser);
            log.info("Zerodha orders sync completed: {}", response.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error syncing orders: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new SyncResponseDTO("FAILED", "Error syncing orders: " + e.getMessage()));
        }
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<ZerodhaHoldingDTO>> getHoldings() {
        try {
            log.info("Fetching Zerodha holdings");
            User currentUser = authService.getCurrentUser();
            List<ZerodhaHoldingDTO> holdings = zerodhaService.getHoldings(currentUser);
            log.info("Retrieved {} holdings", holdings.size());
            return ResponseEntity.ok(holdings);
        } catch (Exception e) {
            log.error("Error fetching holdings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/positions")
    public ResponseEntity<List<ZerodhaPositionDTO>> getPositions() {
        try {
            log.info("Fetching Zerodha positions");
            User currentUser = authService.getCurrentUser();
            List<ZerodhaPositionDTO> positions = zerodhaService.getPositions(currentUser);
            log.info("Retrieved {} positions", positions.size());
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            log.error("Error fetching positions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<List<ZerodhaOrderDTO>> getOrders() {
        try {
            log.info("Fetching Zerodha orders");
            User currentUser = authService.getCurrentUser();
            List<ZerodhaOrderDTO> orders = zerodhaService.getOrders(currentUser);
            log.info("Retrieved {} orders", orders.size());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/connection-status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        try {
            User currentUser = authService.getCurrentUser();
            boolean isConnected = currentUser.getZerodhaAccessToken() != null;
            boolean tokenValid = false;
            
            if (isConnected && currentUser.getTokenExpiry() != null) {
                tokenValid = currentUser.getTokenExpiry().isAfter(LocalDateTime.now());
            }

            return ResponseEntity.ok(Map.of(
                "connected", isConnected,
                "tokenValid", tokenValid,
                "userId", currentUser.getZerodhaUserId() != null ? currentUser.getZerodhaUserId() : "",
                "tokenExpiry", currentUser.getTokenExpiry() != null ? currentUser.getTokenExpiry().toString() : ""
            ));
        } catch (Exception e) {
            log.error("Error checking connection status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}