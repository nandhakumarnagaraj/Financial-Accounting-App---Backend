package com.financial.sync.repository;

import com.financial.sync.entity.XeroStateMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XeroStateMappingRepository extends JpaRepository<XeroStateMapping, String> {
}
