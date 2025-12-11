package com.financial.sync.repository;

import com.financial.sync.entity.ZerodhaOrder;
import com.financial.sync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ZerodhaOrderRepository extends JpaRepository<ZerodhaOrder, Long> {

	List<ZerodhaOrder> findByUser(User user);

	Optional<ZerodhaOrder> findByOrderId(String orderId);
}