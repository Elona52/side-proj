package com.api.auction.service;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.auction.mapper.AuctionMapper;
import com.api.auction.domain.Auction;
import com.api.auction.domain.FindBoard;
import com.api.auction.domain.Reply;
import com.api.item.domain.KamcoItem;
import com.api.item.service.KamcoItemService;
import com.api.item.service.OnbidApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

	private final AuctionMapper auctionMapper;
	private final KamcoItemService kamcoItemService;
	private final OnbidApiService onbidApiService;
	
	private static final int PAGE_GROUP = 10;
	

	@Transactional
	public void insertAuction(Auction at) {
		auctionMapper.insertAuction(at);
	}

	public Map<String, Object> getAuctionList(String printType, String period, 
					String keyword, int pageNum, int pageSize) {
		int currentPage = pageNum;
		int start = (currentPage -1) * pageSize;
		int listCount = auctionMapper.getAuctionCount(period, keyword);
		// 페이지에 출력되는 리스트
		List<Auction> auctionList = auctionMapper.getAuctionList(printType, period, keyword, start, pageSize);

		int pageCount = listCount / pageSize + (listCount % pageSize == 0 ? 0 : 1);
		int startPage = (currentPage / PAGE_GROUP) * PAGE_GROUP + 1
				- (currentPage % PAGE_GROUP == 0 ? PAGE_GROUP : 0);
		
		int endPage = startPage + PAGE_GROUP - 1;
		
		if(endPage > pageCount) {
		endPage = pageCount;
		}
		
		Map<String, Object> modelMap = new HashMap<>();
		modelMap.put("atList", auctionList);
		modelMap.put("currentPage", currentPage);
		modelMap.put("listCount", listCount);
		modelMap.put("pageCount", pageCount);
		modelMap.put("startPage", startPage);
		modelMap.put("endPage", endPage);
		modelMap.put("period", period);
		modelMap.put("keyword", keyword);
		modelMap.put("pageNum", pageNum);
		modelMap.put("printType", printType);
		
		
		return modelMap;
	}

	public Auction getAuction(int no) {
		return auctionMapper.getAuction(no);
	}

	@Transactional
	public Auction bid(int no, String buyer, int endPrice) {
		// DB 업데이트
		auctionMapper.updateEndPrice(no, buyer, endPrice);
		// 경매정보 가져오기
		return auctionMapper.getAuction(no);
	}

	@Transactional
	public void updateAuction(Auction auction) {
		auctionMapper.updateAuction(auction);
	}

	@Transactional
	public void deleteAuction(int no) {
		auctionMapper.deleteAuction(no);
	}

	@Transactional
	public boolean insertBoard(FindBoard board) {
		try {
			auctionMapper.insertBoard(board);
			return true;
		} catch (Exception e) {
			log.error("게시글 저장 중 오류", e);
			return false;
		}
	}

	public List<FindBoard> getBoardList(String id, String keyword, String category) {
		return auctionMapper.getBoardList(id, keyword, category);
	}

	public FindBoard getBoard(int no) {
		// 조회수 증가
		auctionMapper.incrementBoardViews(no);
		return auctionMapper.getBoard(no);
	}
	
	@Transactional
	public void incrementBoardViews(int no) {
		auctionMapper.incrementBoardViews(no);
	}

	@Transactional
	public void updateBoard(FindBoard board) {
		auctionMapper.updateBoard(board);
	}

	@Transactional
	public void deleteBoard(int no) {
		auctionMapper.deleteBoard(no);
	}
	
	@Transactional
	public void insertReply(Reply re) {
		auctionMapper.insertReply(re);
	}
	
	@Transactional
	public void updateReply(Reply re) {
		auctionMapper.updateReply(re);
	}

	@Transactional
	public void deleteReply(int no) {
		auctionMapper.deleteReply(no);
	}
	
	public List<Reply> getReplyList(int boardNo) {
		return auctionMapper.getReplyList(boardNo);
	}

	public List<Auction> getMyAuctionList(String id, String option) {
		return auctionMapper.getMyAuctionList(id, option);
	}

	public List<Auction> getCommissionList(Date start, Date end) {
		return auctionMapper.getCommissionList(start, end);
	}
	
	/**
	 * KamcoItem을 Auction으로 변환
	 */
	public Auction convertKamcoItemToAuction(KamcoItem kamcoItem) {
		if (kamcoItem == null) {
			return null;
		}
		
		Auction auction = new Auction();
		auction.setNo(kamcoItem.getId() != null ? kamcoItem.getId().intValue() : 0);
		auction.setCltrNo(kamcoItem.getCltrNo());
		auction.setName(kamcoItem.getCltrNm() != null ? kamcoItem.getCltrNm() : kamcoItem.getLdnmAdrs());
		auction.setContent(kamcoItem.getGoodsNm());
		auction.setApiItem(true);
		
		// 가격 (마이너스 값 검증)
		Long minBidPrc = kamcoItem.getMinBidPrc();
		if (minBidPrc != null && minBidPrc > 0) {
			auction.setStartPrice(minBidPrc.intValue());
		} else {
			auction.setStartPrice(0);
		}
		Long apslAsesAvgAmt = kamcoItem.getApslAsesAvgAmt();
		if (apslAsesAvgAmt != null && apslAsesAvgAmt > 0) {
			auction.setEndPrice(apslAsesAvgAmt.intValue());
		} else {
			auction.setEndPrice(0);
		}
		
		// 날짜
		try {
			if (kamcoItem.getPbctBegnDtm() != null && kamcoItem.getPbctBegnDtm().length() >= 8) {
				String dateStr = kamcoItem.getPbctBegnDtm().replaceAll("[^0-9]", "");
				if (dateStr.length() >= 8) {
					auction.setStartDate(Date.valueOf(
						dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8)
					));
				}
			}
			if (kamcoItem.getPbctClsDtm() != null && kamcoItem.getPbctClsDtm().length() >= 8) {
				String dateStr = kamcoItem.getPbctClsDtm().replaceAll("[^0-9]", "");
				if (dateStr.length() >= 8) {
					auction.setEndDate(Date.valueOf(
						dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8)
					));
				}
			}
		} catch (Exception e) {
			// 날짜 변환 실패 무시
		}
		
		auction.setRegDate(new Timestamp(System.currentTimeMillis()));
		auction.setCount(kamcoItem.getUscbCnt() != null ? kamcoItem.getUscbCnt() : 0);
		// 이미지는 기본값으로 설정
		auction.setImg("placeholder.svg");
		
		return auction;
	}
	
	/**
	 * KamcoItem 리스트를 Auction 리스트로 변환
	 */
	public List<Auction> convertKamcoItemsToAuctions(List<KamcoItem> kamcoItems) {
		if (kamcoItems == null || kamcoItems.isEmpty()) {
			return new ArrayList<>();
		}
		
		return kamcoItems.stream()
			.map(this::convertKamcoItemToAuction)
			.collect(Collectors.toList());
	}
	
	/**
	 * 메인 페이지 데이터 준비
	 */
	public Map<String, Object> prepareMainPageData() {
		Map<String, Object> data = new HashMap<>();
		
		try {
			// 1. 50% 체감 물건 4개 조회 (DB에서 서울특별시만)
			List<KamcoItem> discount50Items = new ArrayList<>();
			try {
				List<KamcoItem> allDiscount = kamcoItemService.get50PercentDiscountItems(100);
				discount50Items = allDiscount.stream()
					.filter(item -> "서울특별시".equals(item.getSido()))
					.limit(4)
					.collect(Collectors.toList());
			} catch (Exception e) {
				System.err.println("⚠️ 50% 체감 물건 조회 실패: " + e.getMessage());
			}
			
			// Auction 객체로 변환
			List<Auction> discountList = convertKamcoItemsToAuctions(discount50Items);
			data.put("discountList", discountList);
			
			// 2. 용도별 물건 조회 (카테고리 통계 - DB에서 조회)
			Map<String, Integer> categoryStats = new HashMap<>();
			try {
				List<KamcoItem> allItems = kamcoItemService.getBySido("서울특별시");
				for (KamcoItem item : allItems) {
					String usage = item.getCtgrFullNm();
					if (usage != null && !usage.isEmpty()) {
						categoryStats.put(usage, categoryStats.getOrDefault(usage, 0) + 1);
					}
				}
			} catch (Exception e) {
				System.err.println("⚠️ 용도별 물건 조회 실패: " + e.getMessage());
			}
			data.put("categoryStats", categoryStats);
			
			// 3. 마감임박 물건 조회 (DB에서 조회)
			List<Map<String, String>> scheduleList = new ArrayList<>();
			try {
				List<KamcoItem> deadlineItems = kamcoItemService.getTodayClosingItems();
				
				String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				Map<String, Map<String, String>> districtMap = new HashMap<>();
				
				for (KamcoItem item : deadlineItems) {
					if (item.getPbctClsDtm() != null && item.getPbctClsDtm().startsWith(today)) {
						String region = extractRegion(item);
						
						if (!region.isEmpty() && !districtMap.containsKey(region)) {
							Map<String, String> schedule = new HashMap<>();
							schedule.put("cltrNo", item.getCltrNo());
							schedule.put("region", region);
							schedule.put("pbctClsDtm", item.getPbctClsDtm());
							districtMap.put(region, schedule);
							
							if (districtMap.size() >= 5) break;
						}
					}
				}
				scheduleList.addAll(districtMap.values());
			} catch (Exception e) {
				System.err.println("⚠️ 마감임박 물건 조회 실패: " + e.getMessage());
			}
			data.put("scheduleList", scheduleList);
			
			// 4. 신규 물건 조회 (DB에서 조회)
			List<Map<String, Object>> notices = new ArrayList<>();
			try {
				List<KamcoItem> newItems = kamcoItemService.getNewItems(5);
				for (KamcoItem item : newItems) {
					Map<String, Object> notice = new HashMap<>();
					notice.put("cltrNo", item.getCltrNo());
					notice.put("title", item.getCltrNm() != null ? item.getCltrNm() : "신규 물건");
					notice.put("date", item.getCreatedDate() != null ? item.getCreatedDate() : new java.util.Date());
					notices.add(notice);
				}
			} catch (Exception e) {
				System.err.println("⚠️ 신규 물건 조회 실패: " + e.getMessage());
			}
			data.put("notices", notices);
			
			// 5. 통계 정보
			data.put("statsRate", "62.5");
			data.put("statsLabel", "2025년 11월 평균 입찰가율");
			data.put("totalItems", discount50Items.size());
			
		} catch (Exception e) {
			System.err.println("❌ 메인 페이지 데이터 준비 중 오류: " + e.getMessage());
			e.printStackTrace();
			
			// 오류 발생 시 빈 데이터로 초기화
			data.put("discountList", new ArrayList<>());
			data.put("categoryStats", new HashMap<>());
			data.put("scheduleList", new ArrayList<>());
			data.put("notices", new ArrayList<>());
			data.put("statsRate", "0.0");
			data.put("statsLabel", "데이터 로딩 중...");
			data.put("totalItems", 0);
		}
		
		return data;
	}
	
	/**
	 * API 아이템 상세 페이지 데이터 준비
	 */
	public Map<String, Object> prepareApiItemDetailData(String cltrNo) {
		Map<String, Object> data = new HashMap<>();
		
		try {
			com.api.item.domain.KamcoItem item = kamcoItemService.getByCltrNo(cltrNo);
			
			if (item != null) {
				data.put("item", item);
				data.put("itemHistory", item);
				
				// 부동산 여부 확인
				boolean isRealEstate = false;
				if (item.getCtgrFullNm() != null) {
					String category = item.getCtgrFullNm();
					isRealEstate = category.contains("건물") || category.contains("토지") || category.contains("임야") 
						|| category.contains("부동산") || category.contains("주거") || category.contains("상가") 
						|| category.contains("산업") || category.contains("공장");
				}
				data.put("isRealEstate", isRealEstate);
				
				// 주소 정규화
				String normalizedAddress = normalizeAddressForMap(
					item.getNmrdAdrs(), item.getLdnmAdrs(), item.getCltrNm());
				data.put("normalizedAddress", normalizedAddress);
				
				// 디버깅: 주소 정보 로깅
				System.out.println("📍 상세페이지 주소 정보 - cltrNo: " + cltrNo);
				System.out.println("  - nmrdAdrs: " + item.getNmrdAdrs());
				System.out.println("  - ldnmAdrs: " + item.getLdnmAdrs());
				System.out.println("  - cltrNm: " + item.getCltrNm());
				System.out.println("  - normalizedAddress: " + normalizedAddress);
			}
		} catch (Exception e) {
			System.err.println("❌ API 상세 조회 오류: " + e.getMessage());
			e.printStackTrace();
		}
		
		return data;
	}
	
	/**
	 * 신규 물건 목록 페이지 데이터 준비
	 */
	public Map<String, Object> prepareNewItemsPageData(String sido, int pageNum, int pageSize) {
		Map<String, Object> data = new HashMap<>();
		
		try {
			// 신규 물건만 조회 (is_new = 1 조건, 시도별 필터링 포함)
			// 신규 물건이 없으면 빈 리스트 반환 (일반 데이터는 표시하지 않음)
			List<com.api.item.domain.KamcoItem> newItems = kamcoItemService.getNewItemsBySido(sido);
			
			// KamcoItem을 Auction으로 변환
			List<Auction> auctions = convertKamcoItemsToAuctions(newItems);
			
			// 페이지네이션
			int totalCount = auctions.size();
			Map<String, Object> pagination = calculatePagination(totalCount, pageNum, pageSize, 10);
			List<Auction> pagedAuctions = paginateList(auctions, pageNum, pageSize);
			
			data.put("atList", pagedAuctions);
			data.put("category", "신규물건");
			data.put("period", "new");
			data.put("printType", "new");
			data.put("sido", sido);
			data.put("pageNum", pageNum);
			data.putAll(pagination);
			data.put("totalCount", totalCount);
			
		} catch (Exception e) {
			System.err.println("❌ 경매공고 페이지 오류: " + e.getMessage());
			e.printStackTrace();
			data.put("atList", new ArrayList<>());
			data.put("category", "신규물건");
			data.put("period", "new");
			data.put("printType", "new");
			data.put("totalCount", 0);
		}
		
		return data;
	}
	
	/**
	 * 경매 목록 페이지 데이터 준비
	 */
	public Map<String, Object> prepareAuctionListPageData(String category, String period, 
			String printType, String sido, int pageNum, int pageSize) {
		Map<String, Object> data = new HashMap<>();
		
		try {
			// DB에서 시도별 데이터 가져오기
			List<com.api.item.domain.KamcoItem> items = kamcoItemService.getBySido(sido != null ? sido : "서울특별시");
			
			// 카테고리 필터링
			if (category != null && !category.equals("all") && !category.isEmpty()) {
				String searchCategory = category;
				if (category.contains(" / ")) {
					searchCategory = category.substring(category.lastIndexOf(" / ") + 3).trim();
				}
				
				final String finalSearchCategory = searchCategory;
				items = items.stream()
					.filter(item -> {
						if (item.getCtgrFullNm() == null) return false;
						return item.getCtgrFullNm().contains(finalSearchCategory);
					})
					.collect(Collectors.toList());
			}
			
			// KamcoItem을 Auction으로 변환
			List<Auction> auctions = convertKamcoItemsToAuctions(items);
			
			// 페이지네이션
			int totalCount = auctions.size();
			Map<String, Object> pagination = calculatePagination(totalCount, pageNum, pageSize, 10);
			List<Auction> pagedAuctions = paginateList(auctions, pageNum, pageSize);
			
			data.put("atList", pagedAuctions);
			data.put("category", category);
			data.put("period", period);
			data.put("printType", printType);
			data.put("sido", sido);
			data.put("pageNum", pageNum);
			data.putAll(pagination);
			data.put("totalCount", totalCount);
			
		} catch (Exception e) {
			System.err.println("❌ 경매 목록 조회 오류: " + e.getMessage());
			e.printStackTrace();
			data.put("atList", new ArrayList<>());
			data.put("category", category);
			data.put("period", period);
			data.put("printType", printType);
			data.put("totalCount", 0);
		}
		
		return data;
	}
	
	/**
	 * 50% 체감 물건 페이지 데이터 준비
	 */
	public Map<String, Object> prepareDiscount50PageData(String sido, int pageNum, int pageSize) {
		Map<String, Object> data = new HashMap<>();
		
		try {
			// API에서 50% 체감 물건 가져오기
			List<com.api.item.domain.Item> apiItems = new ArrayList<>();
			
			for (int page = 1; page <= 5; page++) {
				try {
					List<com.api.item.domain.Item> pageItems = onbidApiService.getUnifyDegression50PerCltrList(sido, page, 100);
					if (pageItems != null && !pageItems.isEmpty()) {
						apiItems.addAll(pageItems);
					} else {
						break;
					}
					Thread.sleep(300);
				} catch (Exception e) {
					break;
				}
			}
			
			// DB에 저장
			int savedCount = 0;
			if (!apiItems.isEmpty()) {
				savedCount = kamcoItemService.saveBatchFromApiItems(apiItems);
			}
			
			// DB에서 저장된 50% 체감 물건 (시도별 전체) 조회
			List<com.api.item.domain.KamcoItem> discountItems = kamcoItemService.get50PercentDiscountItemsBySido(sido);
			
			// 시도 필터링
			if (sido != null && !sido.isEmpty() && !sido.equals("all")) {
				discountItems = discountItems.stream()
					.filter(item -> sido.equals(item.getSido()))
					.collect(Collectors.toList());
			}
			
			// Auction 객체로 변환
			List<Auction> auctions = convertKamcoItemsToAuctions(discountItems);
			
			// 페이지네이션
			int totalCount = auctions.size();
			Map<String, Object> pagination = calculatePagination(totalCount, pageNum, pageSize, 10);
			List<Auction> pagedAuctions = paginateList(auctions, pageNum, pageSize);
			
			data.put("atList", pagedAuctions);
			data.put("category", "50% 체감 물건");
			data.put("period", "progress");
			data.put("printType", "new");
			data.put("sido", sido);
			data.put("pageNum", pageNum);
			data.putAll(pagination);
			data.put("totalCount", totalCount);
			data.put("apiFetchedCount", apiItems.size());
			data.put("dbSavedCount", savedCount);
			
		} catch (Exception e) {
			System.err.println("❌ 50% 체감 물건 페이지 오류: " + e.getMessage());
			e.printStackTrace();
			data.put("atList", new ArrayList<>());
			data.put("category", "50% 체감 물건");
			data.put("period", "progress");
			data.put("printType", "new");
			data.put("totalCount", 0);
			data.put("apiFetchedCount", 0);
			data.put("dbSavedCount", 0);
		}
		
		return data;
	}
	
	/**
	 * 게시판 목록/상세 페이지 데이터 준비
	 */
	public Map<String, Object> prepareBoardPageData(String id, String keyword, String category,
			Integer no, int pageNum, int pageSize) {
		Map<String, Object> data = new HashMap<>();
		
		// null 값 처리
		if ("null".equals(id)) id = null;
		if ("null".equals(keyword)) keyword = null;
		if ("all".equals(category)) category = null;
		
		List<FindBoard> allBoards = getBoardList(id, keyword, category);
		int totalCount = allBoards.size();
		
		// 페이지네이션 계산
		int pageCount = totalCount > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 1;
		int pageGroup = 10;
		int startPage = ((pageNum - 1) / pageGroup) * pageGroup + 1;
		int endPage = Math.min(startPage + pageGroup - 1, pageCount);
		
		// 페이지별 데이터 추출
		int startIndex = (pageNum - 1) * pageSize;
		int endIndex = Math.min(startIndex + pageSize, totalCount);
		List<FindBoard> pagedBoards = totalCount > 0 ? allBoards.subList(startIndex, endIndex) : new ArrayList<>();
		
		data.put("boardList", pagedBoards);
		data.put("id", id);
		data.put("keyword", keyword);
		data.put("category", category != null ? category : "all");
		data.put("pageNum", pageNum);
		data.put("pageSize", pageSize);
		data.put("pageCount", pageCount);
		data.put("startPage", startPage);
		data.put("endPage", endPage);
		data.put("totalCount", totalCount);
		
		// 게시판 상세
		if (no != null) {
			data.put("board", getBoard(no));
			data.put("replyList", getReplyList(no));
		}
		
		return data;
	}
	
	/**
	 * 페이지네이션 정보 계산 (PaginationUtilService 통합)
	 */
	private Map<String, Object> calculatePagination(int totalCount, int pageNum, int pageSize, int pageGroup) {
		Map<String, Object> pagination = new HashMap<>();
		
		int pageCount = totalCount > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 1;
		int startPage = ((pageNum - 1) / pageGroup) * pageGroup + 1;
		int endPage = Math.min(startPage + pageGroup - 1, pageCount);
		
		pagination.put("pageCount", pageCount);
		pagination.put("startPage", startPage);
		pagination.put("endPage", endPage);
		pagination.put("totalCount", totalCount);
		
		return pagination;
	}
	
	/**
	 * 리스트를 페이지 단위로 분할 (PaginationUtilService 통합)
	 */
	private <T> List<T> paginateList(List<T> list, int pageNum, int pageSize) {
		if (list == null || list.isEmpty()) {
			return new ArrayList<>();
		}
		
		int startIndex = (pageNum - 1) * pageSize;
		int endIndex = Math.min(startIndex + pageSize, list.size());
		
		if (startIndex >= list.size()) {
			return new ArrayList<>();
		}
		
		return list.subList(startIndex, endIndex);
	}

	/**
	 * KamcoItem에서 지역명(구 단위) 추출 (AddressUtilService 통합)
	 */
	private String extractRegion(KamcoItem item) {
		String region = "";

		String ldnmAdrs = item.getLdnmAdrs();
		if (ldnmAdrs != null && !ldnmAdrs.isEmpty()) {
			if (ldnmAdrs.contains("서울특별시")) {
				Pattern pattern = Pattern.compile("서울특별시\\s+([가-힣]+구)");
				Matcher matcher = pattern.matcher(ldnmAdrs);
				if (matcher.find()) {
					region = matcher.group(1);
				}
			}
		}

		if (region.isEmpty()) {
			String nmrdAdrs = item.getNmrdAdrs();
			if (nmrdAdrs != null && !nmrdAdrs.isEmpty()) {
				if (nmrdAdrs.contains("서울특별시")) {
					Pattern pattern = Pattern.compile("서울특별시\\s+([가-힣]+구)");
					Matcher matcher = pattern.matcher(nmrdAdrs);
					if (matcher.find()) {
						region = matcher.group(1);
					}
				}
			}
		}

		if (region.isEmpty()) {
			String cltrNm = item.getCltrNm();
			if (cltrNm != null && !cltrNm.isEmpty()) {
				Pattern pattern = Pattern.compile("(서울특별시\\s+)?([가-힣]+구)");
				Matcher matcher = pattern.matcher(cltrNm);
				if (matcher.find()) {
					String fullDistrict = matcher.group(2);
					if (fullDistrict != null) {
						region = fullDistrict;
					}
				}
			}
		}

		if (region.isEmpty()) {
			String sido = item.getSido();
			if (sido != null && sido.contains("서울")) {
				region = "서울";
			}
		}

		return region;
	}

	/**
	 * 지도 API를 위한 주소 정규화 (AddressUtilService 통합)
	 */
	private String normalizeAddressForMap(String nmrdAdrs, String ldnmAdrs, String cltrNm) {
		String address = "";

		if (nmrdAdrs != null && !nmrdAdrs.trim().isEmpty()) {
			address = nmrdAdrs.trim();
		} else if (ldnmAdrs != null && !ldnmAdrs.trim().isEmpty()) {
			address = ldnmAdrs.trim();
		} else if (cltrNm != null && !cltrNm.trim().isEmpty()) {
			Pattern pattern = Pattern.compile(
				"(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|경기도|강원도|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도|제주특별자치도)[^0-9]*[0-9]"
			);
			Matcher matcher = pattern.matcher(cltrNm);
			if (matcher.find()) {
				address = matcher.group(0);
			}
		}

		if (address.isEmpty()) {
			return "";
		}

		String normalized = address;
		normalized = normalized.replaceAll("\\s*\\([^)]*\\)", "");
		normalized = normalized.replaceAll("\\s*위\\s*(지상\\s*)?건축물\\s*", " ");
		normalized = normalized.replaceAll("\\s+", " ").trim();

		Pattern roadPattern = Pattern.compile(
			"(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|경기도|강원도|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도|제주특별자치도)\\s+([가-힣]+(?:시|군|구))\\s+([가-힣]+(?:\\d+)*(?:로|대로|길|거리))\\s*,?\\s*([\\d-]+)"
		);
		Matcher roadMatcher = roadPattern.matcher(normalized);
		if (roadMatcher.find()) {
			return roadMatcher.group(1) + " " + roadMatcher.group(2) + " " + roadMatcher.group(3) + " " + roadMatcher.group(4);
		}

		Pattern lotPattern = Pattern.compile(
			"(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|경기도|강원도|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도|제주특별자치도)\\s+([가-힣]+(?:시|군|구))\\s+([가-힣]+동)\\s*,?\\s*([\\d-]+)"
		);
		Matcher lotMatcher = lotPattern.matcher(normalized);
		if (lotMatcher.find()) {
			return lotMatcher.group(1) + " " + lotMatcher.group(2) + " " + lotMatcher.group(3) + " " + lotMatcher.group(4);
		}

		return normalized;
	}
}
