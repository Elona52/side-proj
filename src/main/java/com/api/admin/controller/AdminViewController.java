package com.api.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * 어드민 View 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminViewController {

    /**
     * 관리 페이지 메인 화면
     * GET /admin/panel
     */
    @GetMapping("/panel")
    public String adminPanel() {
        log.info("📊 어드민 관리 페이지 접속");
        return "admin/admin-panel";
    }
}

