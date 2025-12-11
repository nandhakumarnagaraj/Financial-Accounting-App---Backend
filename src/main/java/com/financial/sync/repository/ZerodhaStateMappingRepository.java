package com.financial.sync.repository;

import com.financial.sync.entity.ZerodhaStateMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZerodhaStateMappingRepository extends JpaRepository<ZerodhaStateMapping, String> {
	
}