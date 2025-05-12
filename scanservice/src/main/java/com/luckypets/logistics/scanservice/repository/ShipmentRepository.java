package com.luckypets.logistics.scanservice.repository;

import com.luckypets.logistics.scanservice.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository <Shipment, String> {
}
