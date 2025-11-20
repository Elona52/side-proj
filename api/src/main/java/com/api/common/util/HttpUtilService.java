package com.api.common.util;

import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import com.api.member.domain.Member;

/**
 * HTTP 관련 유틸리티 서비스
 */
@Service
public class HttpUtilService {
    
    /**
     * 세션에서 회원 ID 추출
     */
    public String getMemberId(HttpServletRequest request) {
        Member member = (Member) request.getSession().getAttribute("member");
        return member != null ? member.getId() : null;
    }
    
    /**
     * 클라이언트 IP 주소 추출
     */
    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        return ip;
    }
}

