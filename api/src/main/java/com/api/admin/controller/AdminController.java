package com.api.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.api.auction.service.AuctionService;
import com.api.admin.service.AdminService;

@Controller
public class AdminController {

	@Autowired 
	private AuctionService auctionService;
	
	@Autowired
	private AdminService adminService;
	
	@RequestMapping(value= {"adminMemberRegister", "adminMemberUpdate", "adminCommission"})
	public String adminPage(Model model, 
			@RequestParam(name = "startDate", required = false, defaultValue = "1000-01-01") String startDate, 
			@RequestParam(name = "endDate", required = false, defaultValue = "1000-01-01") String endDate) {
		
		java.sql.Date[] dates = adminService.parseDateRange(startDate, endDate);
		model.addAttribute("auctionList", auctionService.getCommissionList(dates[0], dates[1]));
		
		return "admin/adminCommission";
	}
}

