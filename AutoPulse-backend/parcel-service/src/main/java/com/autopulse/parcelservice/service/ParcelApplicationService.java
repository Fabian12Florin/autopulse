package com.autopulse.parcelservice.service;

import com.autopulse.parcelservice.model.enums.RouteType;
import com.autopulse.parcelservice.web.dto.query.ParcelSearchRequest;
import com.autopulse.parcelservice.web.dto.request.CreateParcelRequest;
import com.autopulse.parcelservice.web.dto.request.UpdateParcelRequest;
import com.autopulse.parcelservice.web.dto.request.VerifyDeliveryCodeRequest;
import com.autopulse.parcelservice.web.dto.response.DeliveryCodeVerificationResponse;
import com.autopulse.parcelservice.web.dto.response.ParcelPinResponse;
import com.autopulse.parcelservice.web.dto.response.ParcelResponse;
import com.autopulse.parcelservice.web.dto.response.ParcelSummaryResponse;
import com.autopulse.parcelservice.web.dto.response.RoutableParcelResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ParcelApplicationService {

    ParcelResponse createParcel(CreateParcelRequest req);

    ParcelResponse getParcel(UUID parcelId);

    ParcelResponse getParcelByAwb(String awb);

    ParcelPinResponse getParcelPinByAwb(String awb);

    DeliveryCodeVerificationResponse verifyDeliveryCode(VerifyDeliveryCodeRequest request);

    List<RoutableParcelResponse> getRoutableParcels(String depotCode, RouteType routeType, int limit);

    Page<ParcelSummaryResponse> searchParcels(ParcelSearchRequest criteria);

    ParcelResponse updateParcel(UUID parcelId, UpdateParcelRequest req);
}

