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
        User currentUser = authService.getCurrentUser();

        String state = UUID.randomUUID().toString();

        ZerodhaStateMapping mapping = new ZerodhaStateMapping();
        mapping.setState(state);
        mapping.setUserId(currentUser.getId());
        zerodhaStateMappingRepository.save(mapping);

        String authUrl = zerodhaService.getAuthorizationUrl(state);

        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

    @GetMapping("/callback")
    public ResponseEntity<MessageResponse> handleCallback(
            @RequestParam String request_token,
            @RequestParam String state) {
        try {
            ZerodhaStateMapping mapping = zerodhaStateMappingRepository.findById(state)
                    .orElseThrow(() -> new RuntimeException("Invalid state"));

            Long userId = mapping.getUserId();

            Map<String, String> tokens = zerodhaService.exchangeRequestTokenForAccessToken(request_token, apiSecret);

            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setZerodhaAccessToken(tokens.get("access_token"));
            user.setTokenExpiry(LocalDateTime.now().plusDays(1)); // Zerodha tokens are valid for 1 day
            userService.updateUser(user);

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
        User currentUser = authService.getCurrentUser();
        SyncResponseDTO response = zerodhaService.syncHoldings(currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/positions/sync")
    public ResponseEntity<SyncResponseDTO> syncPositions() {
        User currentUser = authService.getCurrentUser();
        SyncResponseDTO response = zerodhaService.syncPositions(currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/sync")
    public ResponseEntity<SyncResponseDTO> syncOrders() {
        User currentUser = authService.getCurrentUser();
        SyncResponseDTO response = zerodhaService.syncOrders(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<ZerodhaHoldingDTO>> getHoldings() {
        User currentUser = authService.getCurrentUser();
        List<ZerodhaHoldingDTO> holdings = zerodhaService.getHoldings(currentUser);
        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/positions")
    public ResponseEntity<List<ZerodhaPositionDTO>> getPositions() {
        User currentUser = authService.getCurrentUser();
        List<ZerodhaPositionDTO> positions = zerodhaService.getPositions(currentUser);
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<ZerodhaOrderDTO>> getOrders() {
        User currentUser = authService.getCurrentUser();
        List<ZerodhaOrderDTO> orders = zerodhaService.getOrders(currentUser);
        return ResponseEntity.ok(orders);
    }
}