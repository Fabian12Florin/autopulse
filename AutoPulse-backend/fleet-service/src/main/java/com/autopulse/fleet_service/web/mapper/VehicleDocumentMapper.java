package com.autopulse.fleet_service.web.mapper;

import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleDocument;
import com.autopulse.fleet_service.web.dto.vehicle_document.CreateVehicleDocumentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_document.UpdateVehicleDocumentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_document.VehicleDocumentResponse;
import org.springframework.stereotype.Component;

@Component
public class VehicleDocumentMapper {

    public VehicleDocument toEntity(CreateVehicleDocumentRequest request, Vehicle vehicle) {
        VehicleDocument document = new VehicleDocument();
        document.setVehicle(vehicle);
        document.setDocumentType(request.documentType());
        document.setIssuedAt(request.issuedAt());
        document.setExpiresAt(request.expiresAt());
        document.setCost(request.cost());
        document.setDescription(request.description());
        return document;
    }

    public void mapForUpdate(UpdateVehicleDocumentRequest request, VehicleDocument document) {
        document.setDocumentType(request.documentType());
        document.setIssuedAt(request.issuedAt());
        document.setExpiresAt(request.expiresAt());
        document.setCost(request.cost());
        document.setDescription(request.description());
    }

    public VehicleDocumentResponse toResponse(VehicleDocument document) {
        return new VehicleDocumentResponse(
                document.getId(),
                document.getVehicle().getId(),
                document.getDocumentType(),
                document.getIssuedAt(),
                document.getExpiresAt(),
                document.getCost(),
                document.getDescription(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}