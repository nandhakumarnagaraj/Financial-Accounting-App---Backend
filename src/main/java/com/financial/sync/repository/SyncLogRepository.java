package com.financial.sync.repository;

import com.financial.sync.entity.SyncLog;
import com.financial.sync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

	List<SyncLog> findByUser(User user);

	List<SyncLog> findByUserAndSyncType(User user, String syncType);

	List<SyncLog> findByUserAndStatus(User user, String status);

	@Query("SELECT s FROM SyncLog s WHERE s.user = :user ORDER BY s.createdAt DESC")
	List<SyncLog> findRecentSyncsByUser(@Param("user") User user);

	@Query("SELECT s FROM SyncLog s WHERE s.user = :user AND s.createdAt >= :fromDate ORDER BY s.createdAt DESC")
	List<SyncLog> findSyncsByUserAndDateRange(@Param("user") User user, @Param("fromDate") LocalDateTime fromDate);
}