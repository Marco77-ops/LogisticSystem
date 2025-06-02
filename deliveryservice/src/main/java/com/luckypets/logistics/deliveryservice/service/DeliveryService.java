package com.luckypets.logistics.deliveryservice.service;

import com.luckypets.logistics.deliveryservice.model.DeliveryRequest;
import com.luckypets.logistics.deliveryservice.model.DeliveryResponse;
import com.luckypets.logistics.shared.model.ShipmentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for delivery operations.
 */
public interface DeliveryService {

    /**
     * Get all shipments.
     *
     * @return List of all shipments as DeliveryResponse objects
     */
    List<DeliveryResponse> getAllShipments();

    /**
     * Get a shipment by ID.
     *
     * @param shipmentId the ID of the shipment to retrieve
     * @return the shipment as a DeliveryResponse, or empty if not found
     */
    Optional<DeliveryResponse> getShipmentById(String shipmentId);

    /**
     * Get the status of a shipment.
     *
     * @param shipmentId the ID of the shipment
     * @return the status of the shipment, or "Unknown" if not found
     */
    String getShipmentStatus(String shipmentId);

    /**
     * Mark a shipment as delivered.
     *
     * @param request the delivery request containing shipment ID and location
     * @return the updated shipment as a DeliveryResponse
     */
    DeliveryResponse markAsDelivered(DeliveryRequest request);

    /**
     * Find a shipment entity by ID.
     *
     * @param shipmentId the ID of the shipment to find
     * @return the shipment entity, or empty if not found
     */
    Optional<ShipmentEntity> findShipmentEntityById(String shipmentId);
}