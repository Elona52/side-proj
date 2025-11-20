package com.api.test;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

public class ApiTest {
    
    @SuppressWarnings("unchecked")
	public static void main(String[] args) {
        String apiKey = "4a9c9dde8ae2c662f5d7bc484c937ff43129743101222fe93d3a54bc264377e8";
        String apiUrl = "https://apis.data.go.kr/1230000/ScsbizInfoService/getScsbizInfo";
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            String url = apiUrl +
                    "?serviceKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
                    "&pageNo=1" +
                    "&numOfRows=10" +
                    "&resultType=json";
            
            System.out.println("API 호출 URL: " + url);
            
            Map<String, Object> response = restTemplate.getForObject(new URI(url), Map.class);
            
            System.out.println("=== 전체 API 응답 ===");
            System.out.println(response);
            
            if (response != null) {
                System.out.println("\n=== 응답 키 목록 ===");
                response.keySet().forEach(key -> {
                    Object value = response.get(key);
                    System.out.println("키: " + key + ", 값 타입: " + (value != null ? value.getClass().getSimpleName() : "null"));
                });
                
                // response 구조 분석
                Object responseObj = response.get("response");
                if (responseObj instanceof Map) {
                    Map<String, Object> responseData = (Map<String, Object>) responseObj;
                    System.out.println("\n=== response 내부 구조 ===");
                    responseData.keySet().forEach(key -> {
                        Object value = responseData.get(key);
                        System.out.println("키: " + key + ", 값 타입: " + (value != null ? value.getClass().getSimpleName() : "null"));
                    });
                    
                    // header 확인
                    Object headerObj = responseData.get("header");
                    if (headerObj instanceof Map) {
                        Map<String, Object> header = (Map<String, Object>) headerObj;
                        System.out.println("\n=== header 정보 ===");
                        System.out.println("resultCode: " + header.get("resultCode"));
                        System.out.println("resultMsg: " + header.get("resultMsg"));
                    }
                    
                    // body 확인
                    Object bodyObj = responseData.get("body");
                    if (bodyObj instanceof Map) {
                        Map<String, Object> body = (Map<String, Object>) bodyObj;
                        System.out.println("\n=== body 정보 ===");
                        body.keySet().forEach(key -> {
                            Object value = body.get(key);
                            System.out.println("키: " + key + ", 값 타입: " + (value != null ? value.getClass().getSimpleName() : "null"));
                        });
                        
                        // items 확인
                        Object itemsObj = body.get("items");
                        if (itemsObj != null) {
                            System.out.println("\n=== items 정보 ===");
                            System.out.println("items 타입: " + itemsObj.getClass().getSimpleName());
                            System.out.println("items 내용: " + itemsObj);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("API 호출 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}