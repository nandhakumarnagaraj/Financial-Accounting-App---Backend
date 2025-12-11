package com.financial.sync.repository;

import com.financial.sync.entity.ZerodhaPosition;
import com.financial.sync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ZerodhaPositionRepository extends JpaRepository<ZerodhaPosition, Long> {

	List<ZerodhaPosition> findByUser(User user);
}