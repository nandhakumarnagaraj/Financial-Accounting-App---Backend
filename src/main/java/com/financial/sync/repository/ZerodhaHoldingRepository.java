package com.financial.sync.repository;

import com.financial.sync.entity.ZerodhaHolding;
import com.financial.sync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ZerodhaHoldingRepository extends JpaRepository<ZerodhaHolding, Long> {

	List<ZerodhaHolding> findByUser(User user);

	Optional<ZerodhaHolding> findByUserAndTradingSymbol(User user, String tradingSymbol);
}
