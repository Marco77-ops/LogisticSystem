package com.luckypets.logistics.shipmentservice.persistence;

import com.luckypets.logistics.shared.model.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentEntity, String> {
}