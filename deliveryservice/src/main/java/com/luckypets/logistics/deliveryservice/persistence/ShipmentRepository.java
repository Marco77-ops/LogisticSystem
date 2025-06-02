package com.luckypets.logistics.deliveryservice.persistence;

import com.luckypets.logistics.shared.model.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository für ShipmentEntity.
 */
public interface ShipmentRepository
        extends JpaRepository<ShipmentEntity, String> {
    // keine weiteren Methoden nötig für Basis-CRUD
}
