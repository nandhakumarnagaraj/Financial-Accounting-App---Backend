package com.financial.sync.repository;

import com.financial.sync.entity.XeroAccount;
import com.financial.sync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface XeroAccountRepository extends JpaRepository<XeroAccount, Long> {

	Optional<XeroAccount> findByXeroAccountId(String xeroAccountId);

	List<XeroAccount> findByUser(User user);

	List<XeroAccount> findByUserAndAccountType(User user, String accountType);

	List<XeroAccount> findByUserAndStatus(User user, String status);
}