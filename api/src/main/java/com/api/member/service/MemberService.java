package com.api.member.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

import com.api.member.mapper.MemberMapper;
import com.api.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberMapper memberMapper;
	private final BCryptPasswordEncoder passwordEncoder;

	public String processMemberJoin(
			com.api.member.dto.MemberJoinRequest request,
			String pass,
			String mobile1,
			String mobile2,
			String type,
			HttpServletResponse response,
			RedirectAttributes redirectAttributes) {

		try {
			String phone = mobile1 + mobile2;
			request.setPass(pass);
			request.setPhone(phone);
			request.setMarketing(request.getMarketing());

			Member member = request.toMember();
			member.setType(type != null ? type : "USER");
			insertMember(member);

			return "redirect:/memberLogin";
		} catch (RuntimeException e) {
			return writeAlert(response, redirectAttributes, e.getMessage(), "memberJoin");
		} catch (Exception e) {
			return writeAlert(response, redirectAttributes, "회원가입 중 오류가 발생했습니다. 다시 시도해주세요.", "memberJoin");
		}
	}

	public String handleLoginFailure(HttpServletResponse response, RedirectAttributes redirectAttributes) {
		return writeAlert(response, redirectAttributes, "아이디와 비밀번호가 일치하지 않습니다.", "/memberLogin");
	}

	private String writeAlert(HttpServletResponse response, RedirectAttributes redirectAttributes, String message, String redirectPath) {
		try {
			response.setContentType("text/html; charset=utf-8");
			PrintWriter out = response.getWriter();
			out.println("<script>");
			out.println("	alert('" + message + "');");
			out.println("	location.href='" + redirectPath + "'");
			out.println("</script>");
			out.flush();
			return null;
		} catch (IOException ioException) {
			if (redirectAttributes != null) {
				redirectAttributes.addFlashAttribute("error", message);
			}
			if (!redirectPath.startsWith("/")) {
				redirectPath = "/" + redirectPath;
			}
			return "redirect:" + redirectPath;
		}
	}

	@Transactional
	public void insertMember(Member m) {
		try {
			// 비밀번호 검증
			if(m.getPass() == null || m.getPass().trim().isEmpty()) {
				log.error("❌ 회원가입 실패: 비밀번호가 비어있음 - id={}", m.getId());
				throw new IllegalArgumentException("비밀번호를 입력해주세요.");
			}
			
			// 비밀번호 암호화
			String originalPass = m.getPass();
			String encodedPass = passwordEncoder.encode(originalPass);
			m.setPass(encodedPass);
			
			log.info("🔐 비밀번호 암호화: 원본 길이={}, 암호화 길이={}, 시작={}", 
				originalPass.length(), encodedPass.length(), 
				encodedPass.length() > 10 ? encodedPass.substring(0, 10) + "..." : encodedPass);
			
			// DB에 저장
			memberMapper.insertMember(m);
			log.info("✅ 회원가입 성공: id={}, name={}", m.getId(), m.getName());
		} catch (DuplicateKeyException e) {
			log.error("❌ 회원가입 실패 (중복 아이디): id={}", m.getId());
			throw new RuntimeException("이미 사용 중인 아이디입니다.", e);
		} catch (Exception e) {
			log.error("❌ 회원가입 실패: id={}, 오류: {}", m.getId(), e.getMessage(), e);
			throw new RuntimeException("회원가입 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	@Transactional
	public void updateMember(Member m) {
		m.setPass(passwordEncoder.encode(m.getPass()));
		memberMapper.updateMember(m);
	}

	public Member getMemberInfo(String id) {
		return memberMapper.getMemberInfo(id);
	}
	
	public boolean idCheck(String id) {
		Member m = memberMapper.getMemberInfo(id);
		// 아이디가 있으면 false 반환
		if(m != null) return false;
		// 아이디가 없으면 true 반환
		return true;
	}

	public boolean isLogin(String id, String pass) {
		try {
			Member m = memberMapper.getMemberInfo(id);
			if(m == null) {
				log.warn("⚠️ 로그인 실패: 아이디 없음 - id={}", id);
				return false;
			}
			
			// 디버깅: 입력된 비밀번호와 저장된 비밀번호 확인
			log.info("🔍 로그인 시도: id={}", id);
			log.info("   입력된 비밀번호 길이: {}", pass != null ? pass.length() : 0);
			log.info("   저장된 비밀번호 길이: {}", m.getPass() != null ? m.getPass().length() : 0);
			log.info("   저장된 비밀번호 시작: {}", m.getPass() != null && m.getPass().length() > 10 
				? m.getPass().substring(0, 10) + "..." : m.getPass());
			
			// 비밀번호가 null이거나 빈 문자열인지 확인
			if(pass == null || pass.trim().isEmpty()) {
				log.warn("⚠️ 로그인 실패: 입력된 비밀번호가 비어있음 - id={}", id);
				return false;
			}
			
			if(m.getPass() == null || m.getPass().trim().isEmpty()) {
				log.warn("⚠️ 로그인 실패: 저장된 비밀번호가 비어있음 - id={}", id);
				return false;
			}
			
			// BCrypt 해시는 $2a$ 또는 $2b$로 시작해야 함
			if(!m.getPass().startsWith("$2a$") && !m.getPass().startsWith("$2b$")) {
				log.error("❌ 저장된 비밀번호가 BCrypt 형식이 아님! - id={}, pass={}", id, 
					m.getPass() != null && m.getPass().length() > 20 ? m.getPass().substring(0, 20) + "..." : m.getPass());
				return false;
			}
			
			// 비밀번호가 일치하면 true 반환
			boolean matches = passwordEncoder.matches(pass, m.getPass());
			if(matches) {
				log.info("✅ 로그인 성공: id={}, name={}", id, m.getName());
			} else {
				log.warn("⚠️ 로그인 실패: 비밀번호 불일치 - id={}", id);
				log.warn("   입력: [{}], 저장: [{}]", pass, 
					m.getPass() != null && m.getPass().length() > 20 ? m.getPass().substring(0, 20) + "..." : m.getPass());
			}
			return matches;
		} catch (Exception e) {
			log.error("❌ 로그인 중 오류 발생: id={}, 오류: {}", id, e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * 이름과 전화번호로 아이디 찾기
	 */
	public String findIdByNameAndPhone(String name, String phone) {
		return memberMapper.findIdByNameAndPhone(name, phone);
	}
	
	/**
	 * 아이디, 이름, 전화번호로 회원 확인 (비밀번호 찾기용)
	 */
	public Member findMemberByIdNamePhone(String id, String name, String phone) {
		return memberMapper.findMemberByIdNamePhone(id, name, phone);
	}
	
	/**
	 * 비밀번호 재설정
	 */
	@Transactional
	public boolean resetPassword(String id, String newPassword) {
		Member member = memberMapper.getMemberInfo(id);
		if(member == null) return false;
		
		member.setPass(passwordEncoder.encode(newPassword));
		memberMapper.updateMember(member);
		return true;
	}

	public Map<String, Object> buildFindIdResponse(String name, String phone) {
		Map<String, Object> result = new HashMap<>();

		try {
			String foundId = findIdByNameAndPhone(name, phone);
			if(foundId != null && !foundId.isEmpty()) {
				result.put("success", true);
				result.put("id", foundId);
			} else {
				result.put("success", false);
				result.put("message", "일치하는 회원 정보를 찾을 수 없습니다.");
			}
		} catch (Exception e) {
			log.error("아이디 찾기 중 오류", e);
			result.put("success", false);
			result.put("message", "오류가 발생했습니다: " + e.getMessage());
		}

		return result;
	}

	public Map<String, Object> buildFindPasswordResponse(String id, String name, String phone) {
		Map<String, Object> result = new HashMap<>();

		try {
			Member member = findMemberByIdNamePhone(id, name, phone);
			if(member != null) {
				result.put("success", true);
				result.put("message", "회원 정보가 확인되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "일치하는 회원 정보를 찾을 수 없습니다.");
			}
		} catch (Exception e) {
			log.error("비밀번호 찾기 중 오류", e);
			result.put("success", false);
			result.put("message", "오류가 발생했습니다: " + e.getMessage());
		}

		return result;
	}

	public Map<String, Object> buildResetPasswordResponse(String id, String newPassword) {
		Map<String, Object> result = new HashMap<>();

		try {
			boolean success = resetPassword(id, newPassword);
			if(success) {
				result.put("success", true);
				result.put("message", "비밀번호가 성공적으로 변경되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "비밀번호 변경에 실패했습니다.");
			}
		} catch (Exception e) {
			log.error("비밀번호 재설정 중 오류", e);
			result.put("success", false);
			result.put("message", "오류가 발생했습니다: " + e.getMessage());
		}

		return result;
	}
}
