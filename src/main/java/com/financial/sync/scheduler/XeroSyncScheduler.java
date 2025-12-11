package com.financial.sync.scheduler;

import com.financial.sync.entity.User;
import com.financial.sync.repository.UserRepository;
import com.financial.sync.service.XeroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class XeroSyncScheduler {
    
    private final XeroService xeroService;
    private final UserRepository userRepository;
    
    // Run daily at 2 AM (as configured in application.properties)
    @Scheduled(cron = "${xero.sync.cron}")
    public void scheduledSync() {
        log.info("Starting scheduled Xero sync...");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            if (user.getXeroAccessToken() != null && user.getXeroTenantId() != null) {
                try {
                    log.info("Syncing data for user: {}", user.getUsername());
                    
                    // Sync invoices
                    xeroService.syncInvoices(user);
                    log.info("Invoices synced for user: {}", user.getUsername());
                    
                    // Sync accounts
                    xeroService.syncAccounts(user);
                    log.info("Accounts synced for user: {}", user.getUsername());
                    
                    // Sync transactions
                    xeroService.syncTransactions(user);
                    log.info("Transactions synced for user: {}", user.getUsername());
                    
                } catch (Exception e) {
                    log.error("Error syncing data for user {}: {}", user.getUsername(), e.getMessage());
                }
            }
        }
        
        log.info("Scheduled Xero sync completed");
    }
    
    // Optional: Run every hour to check and refresh tokens
    @Scheduled(fixedRate = 3600000) // Every hour
    public void refreshTokensScheduled() {
        log.info("Checking for tokens that need refresh...");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            if (user.getXeroAccessToken() != null && user.getTokenExpiry() != null) {
                try {
                    // Refresh if token is about to expire (within 10 minutes)
                    if (user.getTokenExpiry().minusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
                        log.info("Refreshing token for user: {}", user.getUsername());
                        xeroService.refreshAccessToken(user);
                    }
                } catch (Exception e) {
                    log.error("Error refreshing token for user {}: {}", user.getUsername(), e.getMessage());
                }
            }
        }
    }
}