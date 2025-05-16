package com.luckypets.logistics.shipmentservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentEntity, String> {
    // keine weiteren Methoden nötig für Basis-CRUD
}