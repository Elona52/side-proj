package com.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class IamportConfig {
    
    @Value("${iamport.imp.code:imp00000000}")
    private String impCode;
    
    @Value("${iamport.api.key:test_api_key}")
    private String apiKey;
    
    @Value("${iamport.api.secret:test_api_secret}")
    private String apiSecret;
    
    @Value("${iamport.callback.url:http://localhost:5555/payment/callback}")
    private String callbackUrl;
}
