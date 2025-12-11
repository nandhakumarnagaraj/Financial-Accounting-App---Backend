package com.financial.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.sync.dto.*;
import com.financial.sync.entity.*;
import com.financial.sync.exception.XeroAuthException;
import com.financial.sync.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZerodhaServiceImpl implements ZerodhaService {

    private final ZerodhaHoldingRepository holdingRepository;
    private final ZerodhaPositionRepository positionRepository;
    private final ZerodhaOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SyncLogService syncLogService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${zerodha.api-key}")
    private String apiKey;

    @Value("${zerodha.api-secret}")
    private String apiSecret;

    @Value("${zerodha.redirect-uri}")
    private String redirectUri;

    @Value("${zerodha.login-url}")
    private String loginUrl;

    @Value("${zerodha.api-url}")
    private String apiUrl;

    @Override
    public String getAuthorizationUrl(String state) {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return loginUrl + "?api_key=" + apiKey + "&redirect_params=" + state;
    }

    @Override
    public Map<String, String> exchangeRequestTokenForAccessToken(String requestToken, String apiSecret) {
        try {
            // Generate checksum: SHA-256(api_key + request_token + api_secret)
            String data = apiKey + requestToken + apiSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder checksum = new StringBuilder();
            for (byte b : hash) {
                checksum.append(String.format("%02x", b));
            }

            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.post()
                .uri(apiUrl + "/session/token")
                .header("X-Kite-Version", "3")
                .bodyValue(Map.of(
                    "api_key", apiKey,
                    "request_token", requestToken,
                    "checksum", checksum.toString()
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            String accessToken = jsonNode.get("data").get("access_token").asText();

            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", accessToken);
            return tokens;

        } catch (Exception e) {
            log.error("Error exchanging request token", e);
            throw new XeroAuthException("Failed to exchange request token");
        }
    }

    @Override
    @Transactional
    public SyncResponseDTO syncHoldings(User user) {
        SyncLog syncLog = syncLogService.startSync(user, "ZERODHA_HOLDING");

        try {
            validateAuth(user);

            WebClient webClient = webClientBuilder.build();
            String response = webClient.get()
                .uri(apiUrl + "/portfolio/holdings")
                .header("X-Kite-Version", "3")
                .header("Authorization", "token " + apiKey + ":" + user.getZerodhaAccessToken())
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode holdingsNode = objectMapper.readTree(response).get("data");
            int syncedCount = 0;

            if (holdingsNode != null && holdingsNode.isArray()) {
                for (JsonNode node : holdingsNode) {
                    ZerodhaHolding holding = parseHolding(node, user);
                    
                    Optional<ZerodhaHolding> existing = holdingRepository
                        .findByUserAndTradingSymbol(user, holding.getTradingSymbol());
                    existing.ifPresent(value -> holding.setId(value.getId()));

                    holdingRepository.save(holding);
                    syncedCount++;
                }
            }

            syncLogService.completeSync(syncLog, syncedCount, "SUCCESS", null);
            return new SyncResponseDTO("SUCCESS", "Holdings synced successfully");

        } catch (Exception e) {
            log.error("Error syncing holdings", e);
            syncLogService.completeSync(syncLog, 0, "FAILED", e.getMessage());
            return new SyncResponseDTO("FAILED", e.getMessage());
        }
    }

    @Override
    @Transactional
    public SyncResponseDTO syncPositions(User user) {
        SyncLog syncLog = syncLogService.startSync(user, "ZERODHA_POSITION");

        try {
            validateAuth(user);

            WebClient webClient = webClientBuilder.build();
            String response = webClient.get()
                .uri(apiUrl + "/portfolio/positions")
                .header("X-Kite-Version", "3")
                .header("Authorization", "token " + apiKey + ":" + user.getZerodhaAccessToken())
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode positionsNode = objectMapper.readTree(response).get("data").get("net");
            int syncedCount = 0;

            // Clear existing positions (positions are intraday)
            positionRepository.deleteAll(positionRepository.findByUser(user));

            if (positionsNode != null && positionsNode.isArray()) {
                for (JsonNode node : positionsNode) {
                    ZerodhaPosition position = parsePosition(node, user);
                    positionRepository.save(position);
                    syncedCount++;
                }
            }

            syncLogService.completeSync(syncLog, syncedCount, "SUCCESS", null);
            return new SyncResponseDTO("SUCCESS", "Positions synced successfully");

        } catch (Exception e) {
            log.error("Error syncing positions", e);
            syncLogService.completeSync(syncLog, 0, "FAILED", e.getMessage());
            return new SyncResponseDTO("FAILED", e.getMessage());
        }
    }

    @Override
    @Transactional
    public SyncResponseDTO syncOrders(User user) {
        SyncLog syncLog = syncLogService.startSync(user, "ZERODHA_ORDER");

        try {
            validateAuth(user);

            WebClient webClient = webClientBuilder.build();
            String response = webClient.get()
                .uri(apiUrl + "/orders")
                .header("X-Kite-Version", "3")
                .header("Authorization", "token " + apiKey + ":" + user.getZerodhaAccessToken())
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode ordersNode = objectMapper.readTree(response).get("data");
            int syncedCount = 0;

            if (ordersNode != null && ordersNode.isArray()) {
                for (JsonNode node : ordersNode) {
                    ZerodhaOrder order = parseOrder(node, user);
                    
                    Optional<ZerodhaOrder> existing = orderRepository.findByOrderId(order.getOrderId());
                    existing.ifPresent(value -> order.setId(value.getId()));

                    orderRepository.save(order);
                    syncedCount++;
                }
            }

            syncLogService.completeSync(syncLog, syncedCount, "SUCCESS", null);
            return new SyncResponseDTO("SUCCESS", "Orders synced successfully");

        } catch (Exception e) {
            log.error("Error syncing orders", e);
            syncLogService.completeSync(syncLog, 0, "FAILED", e.getMessage());
            return new SyncResponseDTO("FAILED", e.getMessage());
        }
    }

    private void validateAuth(User user) {
        if (user.getZerodhaAccessToken() == null) {
            throw new XeroAuthException("User not authenticated with Zerodha");
        }
    }

    private ZerodhaHolding parseHolding(JsonNode node, User user) {
        ZerodhaHolding holding = new ZerodhaHolding();
        holding.setTradingSymbol(node.path("tradingsymbol").asText());
        holding.setExchange(node.path("exchange").asText());
        holding.setIsin(node.path("isin").asText());
        holding.setQuantity(node.path("quantity").asInt());
        holding.setAveragePrice(new BigDecimal(node.path("average_price").asText("0")));
        holding.setLastPrice(new BigDecimal(node.path("last_price").asText("0")));
        holding.setPnl(new BigDecimal(node.path("pnl").asText("0")));
        holding.setProduct(node.path("product").asText());
        holding.setUser(user);
        return holding;
    }

    private ZerodhaPosition parsePosition(JsonNode node, User user) {
        ZerodhaPosition position = new ZerodhaPosition();
        position.setTradingSymbol(node.path("tradingsymbol").asText());
        position.setExchange(node.path("exchange").asText());
        position.setProduct(node.path("product").asText());
        position.setQuantity(node.path("quantity").asInt());
        position.setBuyQuantity(node.path("buy_quantity").asInt());
        position.setSellQuantquantity(node.path("sell_quantity").asInt());
        position.setAveragePrice(new BigDecimal(node.path("average_price").asText("0")));
        position.setLastPrice(new BigDecimal(node.path("last_price").asText("0")));
        position.setPnl(new BigDecimal(node.path("pnl").asText("0")));
        position.setUnrealisedPnl(new BigDecimal(node.path("unrealised").asText("0")));
        position.setRealisedPnl(new BigDecimal(node.path("realised").asText("0")));
        position.setUser(user);
        return position;
    }

    private ZerodhaOrder parseOrder(JsonNode node, User user) {
        ZerodhaOrder order = new ZerodhaOrder();
        order.setOrderId(node.path("order_id").asText());
        order.setTradingSymbol(node.path("tradingsymbol").asText());
        order.setExchange(node.path("exchange").asText());
        order.setTransactionType(node.path("transaction_type").asText());
        order.setOrderType(node.path("order_type").asText());
        order.setProduct(node.path("product").asText());
        order.setQuantity(node.path("quantity").asInt());
        order.setPrice(new BigDecimal(node.path("price").asText("0")));
        order.setStatus(node.path("status").asText());
        
        String timestamp = node.path("order_timestamp").asText();
        if (timestamp != null && !timestamp.isEmpty()) {
            order.setOrderTimestamp(LocalDateTime.parse(timestamp.replace(" ", "T")));
        }
        
        order.setUser(user);
        return order;
    }

    @Override
    public List<ZerodhaHoldingDTO> getHoldings(User user) {
        return holdingRepository.findByUser(user).stream()
            .map(this::convertToHoldingDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ZerodhaPositionDTO> getPositions(User user) {
        return positionRepository.findByUser(user).stream()
            .map(this::convertToPositionDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ZerodhaOrderDTO> getOrders(User user) {
        return orderRepository.findByUser(user).stream()
            .map(this::convertToOrderDTO)
            .collect(Collectors.toList());
    }

    private ZerodhaHoldingDTO convertToHoldingDTO(ZerodhaHolding holding) {
        ZerodhaHoldingDTO dto = new ZerodhaHoldingDTO();
        dto.setId(holding.getId());
        dto.setTradingSymbol(holding.getTradingSymbol());
        dto.setExchange(holding.getExchange());
        dto.setIsin(holding.getIsin());
        dto.setQuantity(holding.getQuantity());
        dto.setAveragePrice(holding.getAveragePrice());
        dto.setLastPrice(holding.getLastPrice());
        dto.setPnl(holding.getPnl());
        dto.setProduct(holding.getProduct());
        return dto;
    }

    private ZerodhaPositionDTO convertToPositionDTO(ZerodhaPosition position) {
        ZerodhaPositionDTO dto = new ZerodhaPositionDTO();
        dto.setId(position.getId());
        dto.setTradingSymbol(position.getTradingSymbol());
        dto.setExchange(position.getExchange());
        dto.setProduct(position.getProduct());
        dto.setQuantity(position.getQuantity());
        dto.setBuyQuantity(position.getBuyQuantity());
        dto.setSellQuantity(position.getSellQuantquantity());
        dto.setAveragePrice(position.getAveragePrice());
        dto.setLastPrice(position.getLastPrice());
        dto.setPnl(position.getPnl());
        dto.setUnrealisedPnl(position.getUnrealisedPnl());
        dto.setRealisedPnl(position.getRealisedPnl());
        return dto;
    }

    private ZerodhaOrderDTO convertToOrderDTO(ZerodhaOrder order) {
        ZerodhaOrderDTO dto = new ZerodhaOrderDTO();
        dto.setId(order.getId());
        dto.setOrderId(order.getOrderId());
        dto.setTradingSymbol(order.getTradingSymbol());
        dto.setExchange(order.getExchange());
        dto.setTransactionType(order.getTransactionType());
        dto.setOrderType(order.getOrderType());
        dto.setProduct(order.getProduct());
        dto.setQuantity(order.getQuantity());
        dto.setPrice(order.getPrice());
        dto.setStatus(order.getStatus());
        dto.setOrderTimestamp(order.getOrderTimestamp());
        return dto;
    }
}