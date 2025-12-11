package com.financial.sync.repository;

import com.financial.sync.entity.XeroInvoice;
import com.financial.sync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface XeroInvoiceRepository extends JpaRepository<XeroInvoice, Long> {

	Optional<XeroInvoice> findByXeroInvoiceId(String xeroInvoiceId);

	List<XeroInvoice> findByUser(User user);

	List<XeroInvoice> findByUserAndStatus(User user, String status);

	@Query("SELECT i FROM XeroInvoice i WHERE i.user = :user AND i.invoiceDate BETWEEN :startDate AND :endDate")
	List<XeroInvoice> findByUserAndDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Query("SELECT COUNT(i) FROM XeroInvoice i WHERE i.user = :user")
	Long countByUser(@Param("user") User user);
}
