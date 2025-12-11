package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.dtos.requests.AuthRequest;
import com.haui.tech_shop.dtos.responses.AuthResponse;
//import com.nimbusds.jose.JOSEException;


public interface AuthService {
    AuthResponse authenticate(AuthRequest authRequest) ;
//    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;
}
