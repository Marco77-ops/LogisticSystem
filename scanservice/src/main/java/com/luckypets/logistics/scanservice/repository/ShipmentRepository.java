package com.luckypets.logistics.scanservice.repository;

import com.luckypets.logistics.shared.model.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository <ShipmentEntity, String> {
}
