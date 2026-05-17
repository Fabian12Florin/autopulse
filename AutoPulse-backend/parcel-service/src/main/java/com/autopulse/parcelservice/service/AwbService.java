package com.autopulse.parcelservice.service;

import com.autopulse.parcelservice.web.dto.request.GenerateAwbRequest;

public interface AwbService {

    String generateAwb(GenerateAwbRequest context);
}

