package com.api.member.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.member.domain.Member;
import com.api.member.dto.MemberJoinRequest;
import com.api.member.dto.MemberResponse;
import com.api.member.dto.MemberUpdateRequest;
import com.api.member.service.MemberService;

@Controller
public class MemberController {

	@Autowired
	private MemberService memberService;
	
	// 회원가입폼으로 이동
	@RequestMapping(value="memberJoin", method=RequestMethod.GET)
	public String memberJoin() {
		return "member/join";
	}
	// 아이디 중복체크
	@RequestMapping(value="idCheck.ajax", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Boolean> idCheck(@RequestParam(name = "id") String id) {
		boolean result = memberService.idCheck(id);
		
		Map<String, Boolean> map = new HashMap<>();
		map.put("result", result);
		
		return map;
	}
	
	// 회원가입 완료
	@RequestMapping(value="memberJoin", method=RequestMethod.POST)
	public String memberJoinProcess(MemberJoinRequest request,
			@RequestParam(name = "pass1") String pass,
			@RequestParam(name = "mobile1") String mobile1,
			@RequestParam(name = "mobile2") String mobile2,
			@RequestParam(name = "type", required = false) String type,
			HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		return memberService.processMemberJoin(request, pass, mobile1, mobile2, type, response, redirectAttributes);
	}
	// 로그인폼으로 이동
	@RequestMapping("memberLogin")
	public String memberLogin() {
		return "member/login";
	}
	// 로그인하기
	@RequestMapping(value="login", method=RequestMethod.POST)
	public String isLogin(@RequestParam(name = "id") String id,
						 @RequestParam(name = "pass") String pass,
						 HttpServletResponse response,
								HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		boolean isLogin = memberService.isLogin(id, pass);
		if(isLogin) {
			session.setAttribute("isLogin", isLogin);
			session.setAttribute("loginId", id);
			Member m = memberService.getMemberInfo(id);
			
			session.setAttribute("type", m.getType());
		
			return "redirect:main";
		} else {
			return memberService.handleLoginFailure(response, redirectAttributes);
		}
	}
	// 로그아웃하기
	@RequestMapping(value = "logout", method = {RequestMethod.GET, RequestMethod.POST})
	public String isLogout(HttpSession session) {
		session.invalidate();
		return "redirect:/memberLogin";
	}
	// 정보수정폼으로 이동
	@RequestMapping("memberUpdate")
	public String memberUpdate(HttpSession session) {
		return "member/join";
	}
	// 회원정보 가져오기
	@RequestMapping(value="getMemberInfo", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, MemberResponse> getMemberInfo(@RequestParam(name = "id") String id) {
		Map<String, MemberResponse> param = new HashMap<>();
		
		// Domain 객체를 조회 후 DTO로 변환 (비밀번호 제외)
		Member member = memberService.getMemberInfo(id);
		param.put("member", MemberResponse.from(member));
		
		return param;
	}
	// 회원정보수정
	@RequestMapping(value="memberUpdate", method=RequestMethod.POST)
	public String memberUpdate(MemberUpdateRequest request,
						@RequestParam(name = "pass1") String pass,
						@RequestParam(name = "name") String name,
						@RequestParam(name = "mobile1") String mobile1,
						@RequestParam(name = "mobile2") String mobile2,
						@RequestParam(name = "zipcode") int zipcode,
						@RequestParam(name = "address1") String address1,
						@RequestParam(name = "address2") String address2,
						@RequestParam(name = "marketing") String marketing,
						Model model) {
		String phone = mobile1 + mobile2;
		request.setPass(pass);
		request.setName(name);
		request.setPhone(phone);
		request.setZipcode(zipcode);
		request.setAddress1(address1);
		request.setAddress2(address2);
		request.setMarketing(marketing);
		
		// DTO를 Domain으로 변환
		Member member = request.toMember();
		memberService.updateMember(member);
		
		return "redirect:main";
	}
	
	// 비밀번호 체크
	@RequestMapping(value="isPass", method=RequestMethod.POST)
	@ResponseBody
	public boolean isPass(@RequestParam(name = "id") String id,
						  @RequestParam(name = "pass") String pass) {
		boolean isPass = memberService.isLogin(id, pass);
		return isPass;
	}
	
	// 내 즐겨찾기 페이지로 이동
	@RequestMapping("myFavorites")
	public String myFavorites() {
		// 별도의 마이페이지 템플릿 사용
		return "member/favorites";
	}
	
	// 아이디 찾기 페이지
	@RequestMapping("findId")
	public String findIdPage() {
		return "member/findId";
	}
	
	// 아이디 찾기 처리
	@RequestMapping(value="findId", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> findId(@RequestParam(name = "name") String name, 
									 @RequestParam(name = "mobile1") String mobile1,
									 @RequestParam(name = "mobile2") String mobile2) {
		String phone = mobile1 + mobile2;
		return memberService.buildFindIdResponse(name, phone);
	}
	
	// 비밀번호 찾기 페이지
	@RequestMapping("findPassword")
	public String findPasswordPage() {
		return "member/findPassword";
	}
	
	// 비밀번호 찾기 처리 (회원 확인)
	@RequestMapping(value="findPassword", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> findPassword(@RequestParam(name = "id") String id,
											 @RequestParam(name = "name") String name,
											 @RequestParam(name = "mobile1") String mobile1,
											 @RequestParam(name = "mobile2") String mobile2) {
		String phone = mobile1 + mobile2;
		return memberService.buildFindPasswordResponse(id, name, phone);
	}
	
	// 비밀번호 재설정
	@RequestMapping(value="resetPassword", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> resetPassword(@RequestParam(name = "id") String id,
											 @RequestParam(name = "newPassword") String newPassword) {
		return memberService.buildResetPasswordResponse(id, newPassword);
	}
}

