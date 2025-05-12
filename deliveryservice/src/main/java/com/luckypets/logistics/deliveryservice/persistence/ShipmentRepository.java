package com.luckypets.logistics.deliveryservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository für ShipmentEntity.
 */
public interface ShipmentRepository
        extends JpaRepository<ShipmentEntity, String> {
    // keine weiteren Methoden nötig für Basis-CRUD
}
