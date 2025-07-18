package com.luckypets.logistics.shipmentservice.service;

import com.luckypets.logistics.shipmentservice.model.ShipmentRequest;
import com.luckypets.logistics.shipmentservice.model.ShipmentEntity;

import java.util.List;
import java.util.Optional;

public interface ShipmentService {
    ShipmentEntity createShipment(ShipmentRequest request);
    Optional<ShipmentEntity> getShipmentById(String shipmentId);
    List<ShipmentEntity> getAllShipments();
    boolean deleteShipment(String shipmentId);
}