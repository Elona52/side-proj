package com.api.auction.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;

import com.api.auction.domain.Auction;
import com.api.auction.domain.FindBoard;
import com.api.auction.domain.Reply;
import com.api.auction.dto.AuctionResponse;
import com.api.auction.service.AuctionService;

@Controller
public class AuctionController {

	@RequestMapping(value = "/")
	public String root() {
		return "redirect:/main";
	}

	@Autowired
	private AuctionService auctionService;

	@RequestMapping(value = "/main")
	public String mainPage(Model model) {
		System.out.println("\n=== 메인 페이지 데이터 로딩 시작 (DB 데이터 사용) ===");

		Map<String, Object> mainPageData = auctionService.prepareMainPageData();

		// 모델에 데이터 추가
		model.addAttribute("discountList", mainPageData.get("discountList"));
		model.addAttribute("categoryStats", mainPageData.get("categoryStats"));
		model.addAttribute("scheduleList", mainPageData.get("scheduleList"));
		model.addAttribute("notices", mainPageData.get("notices"));
		model.addAttribute("statsRate", mainPageData.get("statsRate"));
		model.addAttribute("statsLabel", mainPageData.get("statsLabel"));
		model.addAttribute("totalItems", mainPageData.get("totalItems"));

		System.out.println("✅ 메인 페이지 데이터 로딩 완료");

		return "main";
	}

	/**
	 * 기존 스타일 메인 페이지 (백업용) - 사용 안 함
	 */
	@RequestMapping(value = "/main-old")
	public String mainPageOld(Model model,
			@RequestParam(name = "pageNum", defaultValue = "1", required = false) int pageNum) {

		// 새로운 메인 페이지로 리다이렉트
		return "redirect:/main";
	}

	// 경매품 상세보기
	@RequestMapping(value = "/detail")
	public String detail(Model model, @RequestParam(name = "no", required = false) Integer no,
			@RequestParam(name = "cltrNo", required = false) String cltrNo,
			@RequestParam(name = "period", defaultValue = "progress") String period,
			@RequestParam(name = "keyword", defaultValue = "null") String keyword) {

		// API 아이템인 경우
		if (cltrNo != null && !cltrNo.isEmpty()) {
			return "redirect:/api-item-detail?cltrNo=" + cltrNo + "&period=" + period + "&keyword=" + keyword;
		}

		// DB 아이템인 경우
		if (no != null) {
			model.addAttribute("auction", auctionService.getAuction(no));
			model.addAttribute("period", period);
			model.addAttribute("keyword", keyword);
			return "auction/api-detail";
		}

		// no와 cltrNo 둘 다 없으면 목록으로
		return "redirect:auctionList";
	}

	// 경매품 상세보기 (기존 스타일)
	@RequestMapping(value = "/auctionDetail")
	public String auctionDetailOld(Model model, @RequestParam(name = "no") int no) {
		model.addAttribute("auction", auctionService.getAuction(no));
		return "auction/api-detail";
	}

	// API 아이템 상세보기 - DB 데이터 사용
	@RequestMapping(value = "/api-item-detail")
	public String apiItemDetail(Model model, @RequestParam(name = "cltrNo") String cltrNo,
			@RequestParam(name = "period", defaultValue = "progress") String period,
			@RequestParam(name = "keyword", defaultValue = "null") String keyword) {

		Map<String, Object> data = auctionService.prepareApiItemDetailData(cltrNo);

		if (data.get("item") == null) {
			return "redirect:/auctionList";
		}

		model.addAllAttributes(data);
		model.addAttribute("period", period);
		model.addAttribute("keyword", keyword);

		return "auction/api-detail";
	}

	// 신규 물건 목록 (경매공고) - DB에서 조회
	@RequestMapping(value = "/new-items")
	public String newItems(Model model,
			@RequestParam(name = "sido", defaultValue = "all", required = false) String sido,
			@RequestParam(name = "pageNum", defaultValue = "1", required = false) int pageNum) {

		int pageSize = 20;
		Map<String, Object> data = auctionService.prepareNewItemsPageData(sido, pageNum, pageSize);
		model.addAllAttributes(data);

		return "auction/list";
	}

	// 경매리스트 출력 - 온비드 API 직접 연동
	@RequestMapping(value = "/auctionList")
	public String progress(Model model,
			@RequestParam(name = "category", defaultValue = "all", required = false) String category,
			@RequestParam(name = "period", defaultValue = "progress", required = false) String period,
			@RequestParam(name = "printType", defaultValue = "new", required = false) String printType,
			@RequestParam(name = "sido", defaultValue = "서울특별시", required = false) String sido,
			@RequestParam(name = "pageNum", defaultValue = "1", required = false) int pageNum) {

		int pageSize = 20;
		Map<String, Object> data = auctionService.prepareAuctionListPageData(category, period, printType, sido, pageNum,
				pageSize);
		model.addAllAttributes(data);

		return "auction/list";
	}

	/**
	 * 50% 체감 물건 목록 페이지 API에서 데이터를 가져와 DB에 저장한 후, 저장된 데이터만 표시
	 */
	@RequestMapping(value = "/discount-50")
	public String discount50Page(Model model,
			@RequestParam(name = "sido", defaultValue = "서울특별시", required = false) String sido,
			@RequestParam(name = "pageNum", defaultValue = "1", required = false) int pageNum) {

		int pageSize = 20;
		Map<String, Object> data = auctionService.prepareDiscount50PageData(sido, pageNum, pageSize);
		model.addAllAttributes(data);

		return "auction/list";
	}

	// 응찰하기
	@RequestMapping(value = "bid", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> bid(@RequestParam(name = "no") int no, @RequestParam(name = "buyer") String buyer,
			@RequestParam(name = "endPrice") int endPrice) {
		auctionService.bid(no, buyer, endPrice);

		Map<String, Object> params = new HashMap<>();
		// Domain을 DTO로 변환하여 응답
		Auction auction = auctionService.getAuction(no);
		params.put("auction", AuctionResponse.from(auction));

		return params;
	}

	@RequestMapping({ "boardList", "boardDetail" })
	public String boardPage(HttpSession session, Model model,
			@RequestParam(name = "id", required = false, defaultValue = "null") String id,
			@RequestParam(name = "keyword", required = false, defaultValue = "null") String keyword,
			@RequestParam(name = "category", required = false, defaultValue = "all") String category,
			@RequestParam(name = "no", required = false) Integer no,
			@RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {

		if (session != null) {
			model.addAttribute("session", session);
		}

		Map<String, Object> data = auctionService.prepareBoardPageData(id, keyword, category, no, pageNum, pageSize);
		model.addAllAttributes(data);

		return "board/board-faq-list";
	}

	// 글 등록하기
	@RequestMapping(value = "insertFindBoard", method = RequestMethod.POST)
	public String insertFindBoard(FindBoard board, HttpSession session) {

		Boolean isLogin = (Boolean) session.getAttribute("isLogin");
		String loginId = (String) session.getAttribute("loginId");

		// 로그인 안 되어 있으면 로그인 페이지로 리다이렉트
		if (isLogin == null || !isLogin || loginId == null) {
			return "redirect:/memberLogin";
		}

		// 세션의 로그인 아이디를 작성자로 설정
		board.setId(loginId);

		auctionService.insertBoard(board);
		return "redirect:/boardFaq";
	}

	// 게시판 수정폼으로 이동
	@RequestMapping("updateBoard")
	public String updateBoard(Model model,
			@RequestParam(name = "id", required = false, defaultValue = "null") String id,
			@RequestParam(name = "keyword", required = false, defaultValue = "null") String keyword,
			@RequestParam(name = "category", required = false, defaultValue = "all") String category,
			@RequestParam(name = "no") int no) {
		if ("null".equals(id))
			id = null;
		if ("null".equals(keyword))
			keyword = null;
		if ("all".equals(category))
			category = null;
		model.addAttribute("boardList", auctionService.getBoardList(id, keyword, category));
		model.addAttribute("id", id);
		model.addAttribute("keyword", keyword);
		model.addAttribute("category", category != null ? category : "all");
		model.addAttribute("board", auctionService.getBoard(no));
		// 수정 폼도 FAQ 레이아웃 템플릿 재사용
		return "board/board-faq-list";
	}

	// 게시글 수정하기
	@RequestMapping(value = "updateFindBoard", method = RequestMethod.POST)
	public String updateFindBoard(Model model, FindBoard board) {
		auctionService.updateBoard(board);
		model.addAttribute("board", auctionService.getBoard(board.getNo()));
		return "redirect:boardDetail?no=" + board.getNo();
	}

	// 게시글 삭제하기
	@RequestMapping("deleteBoard")
	public String deleteFindBoard(@RequestParam(name = "no") int no) {
		auctionService.deleteBoard(no);
		return "redirect:boardList";
	}

	// 게시글 작성 폼 (로그인 사용자 전용)
	@RequestMapping(value = "writeBoard", method = RequestMethod.GET)
	public String writeBoardForm(HttpSession session, Model model) {
		Boolean isLogin = (Boolean) session.getAttribute("isLogin");
		String loginId = (String) session.getAttribute("loginId");

		// 로그인 안 되어 있으면 로그인 페이지로
		if (isLogin == null || !isLogin || loginId == null) {
			return "redirect:/memberLogin";
		}

		// 세션 정보를 모델에 추가 (템플릿에서 사용)
		if (session != null) {
			model.addAttribute("session", session);
		}
		model.addAttribute("loginId", loginId);
		return "board/board-write";
	}

	// 댓글 달기
	@RequestMapping(value = "insertReply.ajax", method = RequestMethod.POST)
	@ResponseBody
	public List<Reply> insertReply(@RequestParam(name = "id") String id, @RequestParam(name = "content") String content,
			@RequestParam(name = "boardNo") int boardNo) {
		// 댓글 인서트 하기
		Reply re = new Reply();
		re.setId(id);
		re.setContent(content);
		re.setBoardNo(boardNo);
		auctionService.insertReply(re);
		// 댓글 리스트 가져오기
		return auctionService.getReplyList(boardNo);
	}

	// 댓글 수정하기
	@RequestMapping(value = "updateReply.ajax", method = RequestMethod.POST)
	@ResponseBody
	public List<Reply> updateReply(@RequestParam(name = "no") int no, @RequestParam(name = "id") String id,
			@RequestParam(name = "content") String content, @RequestParam(name = "boardNo") int boardNo) {
		// 댓글 업데이트하기
		Reply re = new Reply();
		re.setId(id);
		re.setContent(content);
		re.setBoardNo(boardNo);
		re.setNo(no);
		auctionService.updateReply(re);
		// 댓글 리스트 가져오기
		return auctionService.getReplyList(boardNo);
	}

	// 댓글 삭제하기
	@RequestMapping(value = "deleteReply.ajax", method = RequestMethod.POST)
	@ResponseBody
	public List<Reply> deleteReply(@RequestParam(name = "no") int no, @RequestParam(name = "boardNo") int boardNo) {
		// 댓글 삭제하기
		auctionService.deleteReply(no);
		// 댓글 리스트 가져오기
		return auctionService.getReplyList(boardNo);
	}

	// FAQ 게시판 - 물건상세검색 템플릿 레이아웃 사용
	@RequestMapping("boardFaq")
	public String boardFaq(HttpSession session, Model model,
			@RequestParam(name = "id", required = false, defaultValue = "null") String id,
			@RequestParam(name = "keyword", required = false, defaultValue = "null") String keyword,
			@RequestParam(name = "category", required = false, defaultValue = "all") String category,
			@RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {

		if (session != null) {
			model.addAttribute("session", session);
		}

		Map<String, Object> data = auctionService.prepareBoardPageData(id, keyword, category, null, pageNum, pageSize);
		model.addAllAttributes(data);

		return "board/board-faq-list";
	}

	// 내 응찰목록 보기
	@RequestMapping("myAuctionList")
	public String getMyAuctionList(Model model, @RequestParam(name = "id") String id,
			@RequestParam(name = "option", required = false) String option, HttpServletRequest request) {
		model.addAttribute("bidList", auctionService.getMyAuctionList(id, option));
		model.addAttribute("option", option);
		request.setAttribute("bidList", auctionService.getMyAuctionList(id, option));
		return "member/favorites";
	}

	// 경매과정 안내페이지
	@GetMapping("/information")
	public String information() {
		return "auction/information";
	}

	// 입찰참가 안내 페이지
	@GetMapping("/bidding-guide")
	public String biddingGuide() {
		return "auction/bidding-guide";
	}

	// 낙찰 후 절차 안내 페이지 (압류재산)
	@GetMapping("/post-bid-procedure")
	public String postBidProcedure() {
		return "auction/post-bid-procedure";
	}

	// 국유재산 낙찰 후 절차 안내 페이지
	@GetMapping("/national-property-procedure")
	public String nationalPropertyProcedure() {
		return "auction/national-property-procedure";
	}

	// 수탁재산, 유입·유동화자산 낙찰 후 절차 안내 페이지
	@GetMapping("/entrusted-property-procedure")
	public String entrustedPropertyProcedure() {
		return "auction/entrusted-property-procedure";
	}

	// 사이트맵 페이지
	@GetMapping("/sitemap")
	public String sitemap() {
		return "sitemap";
	}

	// 가격 알림 페이지
	@GetMapping("/price-alerts")
	public String priceAlerts(HttpSession session) {
		String userId = (String) session.getAttribute("loginId");
		if (userId == null || userId.isEmpty()) {
			return "redirect:/memberLogin";
		}
		return "member/price-alerts";
	}

}
