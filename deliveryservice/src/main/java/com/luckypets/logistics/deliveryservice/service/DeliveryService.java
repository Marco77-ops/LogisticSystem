package com.luckypets.logistics.deliveryservice.service;

import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.deliveryservice.model.ShipmentEntity;

import java.util.List;
import java.util.Optional;

public interface DeliveryService {

    List<DeliveryResponse> getAllShipments();

    Optional<DeliveryResponse> getShipmentById(String shipmentId);

    String getShipmentStatus(String shipmentId);

    DeliveryResponse markAsDelivered(DeliveryRequest request);

    Optional<ShipmentEntity> findShipmentEntityById(String shipmentId);

    void updateShipmentState(ShipmentEntity shipment);
}
