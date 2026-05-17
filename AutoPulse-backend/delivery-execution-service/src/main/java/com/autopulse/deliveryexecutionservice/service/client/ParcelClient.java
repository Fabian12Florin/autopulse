package com.autopulse.deliveryexecutionservice.service.client;

import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelDeliveryCodeVerificationRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelDeliveryCodeVerificationResponse;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelDeliveryResultRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelFailureResultRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelRejectionResultRequest;
import com.autopulse.deliveryexecutionservice.web.dto.parcel.ParcelWaitingPickupRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "parcel-service")
public interface ParcelClient {

    @PostMapping("/api/parcels/verify-delivery-code")
    ParcelDeliveryCodeVerificationResponse verifyDeliveryCode(
            @RequestBody ParcelDeliveryCodeVerificationRequest request
    );

    @PostMapping("/api/parcels/{parcelId}/out-for-delivery")
    void markOutForDelivery(@PathVariable UUID parcelId);

    @PostMapping("/api/parcels/{parcelId}/delivered")
    void markDelivered(@PathVariable UUID parcelId, @RequestBody ParcelDeliveryResultRequest request);

    @PostMapping("/api/parcels/{parcelId}/failed")
    void markFailed(@PathVariable UUID parcelId, @RequestBody ParcelFailureResultRequest request);

    @PostMapping("/api/parcels/{parcelId}/rejected")
    void markRejected(@PathVariable UUID parcelId, @RequestBody ParcelRejectionResultRequest request);

    @PostMapping("/api/parcels/{parcelId}/waiting-pickup")
    void markWaitingPickup(@PathVariable UUID parcelId, @RequestBody ParcelWaitingPickupRequest request);
}
