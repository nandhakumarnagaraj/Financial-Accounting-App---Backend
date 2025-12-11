package com.financial.sync.scheduler;

import com.financial.sync.entity.User;
import com.financial.sync.repository.UserRepository;
import com.financial.sync.service.ZerodhaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZerodhaSyncScheduler {
    
    private final ZerodhaService zerodhaService;
    private final UserRepository userRepository;
    
    // Run every day at 3 AM (after Xero sync)
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledSync() {
        log.info("Starting scheduled Zerodha sync...");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            if (user.getZerodhaAccessToken() != null) {
                try {
                    log.info("Syncing Zerodha data for user: {}", user.getUsername());
                    
                    // Sync holdings
                    zerodhaService.syncHoldings(user);
                    log.info("Holdings synced for user: {}", user.getUsername());
                    
                    // Sync positions
                    zerodhaService.syncPositions(user);
                    log.info("Positions synced for user: {}", user.getUsername());
                    
                    // Sync orders
                    zerodhaService.syncOrders(user);
                    log.info("Orders synced for user: {}", user.getUsername());
                    
                } catch (Exception e) {
                    log.error("Error syncing Zerodha data for user {}: {}", user.getUsername(), e.getMessage());
                }
            }
        }
        
        log.info("Scheduled Zerodha sync completed");
    }
}