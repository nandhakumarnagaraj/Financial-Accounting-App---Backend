package com.financial.sync.repository;

import com.financial.sync.entity.XeroTransaction;
import com.financial.sync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface XeroTransactionRepository extends JpaRepository<XeroTransaction, Long> {

	Optional<XeroTransaction> findByXeroTransactionId(String xeroTransactionId);

	List<XeroTransaction> findByUser(User user);

	List<XeroTransaction> findByUserAndTransactionType(User user, String transactionType);

	@Query("SELECT t FROM XeroTransaction t WHERE t.user = :user AND t.transactionDate BETWEEN :startDate AND :endDate")
	List<XeroTransaction> findByUserAndDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);
}
