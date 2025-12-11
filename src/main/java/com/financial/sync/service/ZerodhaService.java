package com.financial.sync.service;

import com.financial.sync.dto.*;
import com.financial.sync.entity.User;
import java.util.List;
import java.util.Map;

public interface ZerodhaService {
    String getAuthorizationUrl(String state);
    Map<String, String> exchangeRequestTokenForAccessToken(String requestToken, String apiSecret);
    SyncResponseDTO syncHoldings(User user);
    SyncResponseDTO syncPositions(User user);
    SyncResponseDTO syncOrders(User user);
    List<ZerodhaHoldingDTO> getHoldings(User user);
    List<ZerodhaPositionDTO> getPositions(User user);
    List<ZerodhaOrderDTO> getOrders(User user);
}