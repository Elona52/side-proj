package com.api.item.service;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.api.item.domain.Item;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OnbidApiService {

    private final RestTemplate restTemplate;
    
    public OnbidApiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Value("${ONBID_API_SERVICE_KEY:}")
    private String serviceKeyFromEnv;
    
    @Value("${onbid.api.service-key:}")
    private String serviceKeyFromProps;
    
    private String getServiceKey() {
        // 환경 변수를 우선 사용
        if (serviceKeyFromEnv != null && !serviceKeyFromEnv.trim().isEmpty()) {
            return serviceKeyFromEnv;
        }
        return serviceKeyFromProps != null && !serviceKeyFromProps.trim().isEmpty() ? serviceKeyFromProps : "";
    }
    
    @Value("${ONBID_API_BASE_URL:}")
    private String baseUrlFromEnv;
    
    @Value("${onbid.api.base-url:https://www.onbid.co.kr/op/nsclbi}")
    private String baseUrlFromProps;
    
    private String getBaseUrl() {
        // 환경 변수를 우선 사용
        if (baseUrlFromEnv != null && !baseUrlFromEnv.trim().isEmpty()) {
            return baseUrlFromEnv;
        }
        return baseUrlFromProps;
    }
    
    private String getKamcoApiKey() {
        // 환경 변수 우선, 없으면 서비스 키 사용
        String kamcoKeyFromEnv = System.getenv("KAMCO_API_KEY");
        if (kamcoKeyFromEnv != null && !kamcoKeyFromEnv.trim().isEmpty()) {
            return kamcoKeyFromEnv;
        }
        return getServiceKey();
    }
    
    private String getKamcoApiUrl() {
        // 환경 변수 우선, 없으면 기본 URL + 경로
        String kamcoUrlFromEnv = System.getenv("KAMCO_API_URL");
        if (kamcoUrlFromEnv != null && !kamcoUrlFromEnv.trim().isEmpty()) {
            return kamcoUrlFromEnv;
        }
        return getBaseUrl() + "/getUnifyUsageCltr";
    }

    // 캐시 비활성화 (실시간 데이터 조회를 위해)    // @org.springframework.cache.annotation.Cacheable(value = "onbidItems", key = "'usage_' + #sido + '_' + #pageNo + '_' + #numOfRows")
    public List<Item> getUnifyUsageCltr(String sido, int pageNo, int numOfRows) {
        try {
            String encodedSido = URLEncoder.encode(sido, StandardCharsets.UTF_8);
            String url = getBaseUrl() + "/getUnifyUsageCltr" +
                    "?serviceKey=" + getServiceKey() +
                    "&SIDO=" + encodedSido +
                    "&pageNo=" + pageNo +
                    "&numOfRows=" + numOfRows;
            
            log.info("🔗 API 호출: 통합용도별물건목록조회");
            log.info("   URL: {}", url);
            log.info("   페이지: {}, 개수: {}", pageNo, numOfRows);
            
            String xmlResponse = restTemplate.getForObject(new URI(url), String.class);
            
            if (xmlResponse != null && xmlResponse.length() > 500) {
                log.info("   XML 응답 길이: {} bytes", xmlResponse.length());
                // XML 응답의 일부만 로깅 (너무 길면 잘라서)
                String preview = xmlResponse.length() > 1000 ? xmlResponse.substring(0, 1000) + "..." : xmlResponse;
                log.debug("   XML 응답 미리보기: {}", preview);
            } else {
                log.warn("   ⚠️ XML 응답이 비어있거나 너무 짧음: {}", xmlResponse != null ? xmlResponse.length() : 0);
            }
            
            List<Item> items = parseItemList(xmlResponse);
            log.info("   ✅ 파싱 결과: {}개 아이템 반환", items.size());
            
            return items;
        } catch (Exception e) {
            log.error("❌ 통합용도별물건목록조회 오류: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Item> getUnifyNewCltrList(String sido, int pageNo, int numOfRows) {
        try {
            String encodedSido = URLEncoder.encode(sido, StandardCharsets.UTF_8);
            String url = getBaseUrl() + "/getUnifyNewCltrList" +
                    "?serviceKey=" + getServiceKey() +
                    "&SIDO=" + encodedSido +
                    "&pageNo=" + pageNo +
                    "&numOfRows=" + numOfRows;
            
            log.info("🔗 API 호출: 통합새로운물건목록조회 - {}", url);
            String xmlResponse = restTemplate.getForObject(new URI(url), String.class);
            
            return parseItemList(xmlResponse);
        } catch (Exception e) {
            log.error("❌ 통합새로운물건목록조회 오류: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Item> getUnifyDeadlineCltrList(String sido, int pageNo, int numOfRows) {
        try {
            String encodedSido = URLEncoder.encode(sido, StandardCharsets.UTF_8);
            String url = getBaseUrl() + "/getUnifyDeadlineCltrList" +
                    "?serviceKey=" + getServiceKey() +
                    "&SIDO=" + encodedSido +
                    "&pageNo=" + pageNo +
                    "&numOfRows=" + numOfRows;
            
            log.info("🔗 API 호출: 통합마감임박물건목록조회 - {}", url);
            String xmlResponse = restTemplate.getForObject(new URI(url), String.class);
            
            return parseItemList(xmlResponse);
        } catch (Exception e) {
            log.error("❌ 통합마감임박물건목록조회 오류: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Item> getUnifyDegression50PerCltrList(String sido, int pageNo, int numOfRows) {
        try {
            String encodedSido = URLEncoder.encode(sido, StandardCharsets.UTF_8);
            String url = getBaseUrl() + "/getUnifyDegression50PerCltrList" +
                    "?serviceKey=" + getServiceKey() +
                    "&SIDO=" + encodedSido +
                    "&pageNo=" + pageNo +
                    "&numOfRows=" + numOfRows;
            
            log.info("🔗 API 호출: 통합50%체감물건목록조회 - {}", url);
            String xmlResponse = restTemplate.getForObject(new URI(url), String.class);
            
            return parseItemList(xmlResponse);
        } catch (Exception e) {
            log.error("❌ 통합50%체감물건목록조회 오류: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<Item> parseItemList(String xmlResponse) {
        List<Item> items = new ArrayList<>();
        
        try {
            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.warn("⚠️ XML 응답이 null이거나 비어있음");
                return items;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));
            
            // totalCount 확인
            NodeList totalCountNodes = doc.getElementsByTagName("totalCount");
            if (totalCountNodes.getLength() > 0) {
                String totalCount = totalCountNodes.item(0).getTextContent();
                log.info("📊 API 응답 totalCount: {}", totalCount);
            }
            
            NodeList itemNodes = doc.getElementsByTagName("item");
            log.info("📦 XML에서 발견된 item 노드 개수: {}", itemNodes.getLength());
            
            if (itemNodes.getLength() == 0) {
                log.warn("⚠️ XML에 item 노드가 없습니다. 응답 구조를 확인하세요.");
                // 에러 메시지 확인
                NodeList resultMsgNodes = doc.getElementsByTagName("resultMsg");
                if (resultMsgNodes.getLength() > 0) {
                    String resultMsg = resultMsgNodes.item(0).getTextContent();
                    log.warn("   API resultMsg: {}", resultMsg);
                }
            }
            
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element itemElement = (Element) itemNodes.item(i);
                Item item = parseItemElement(itemElement);
                if (item != null) {
                    items.add(item);
                } else {
                    log.warn("⚠️ item 파싱 실패: 인덱스 {}", i);
                }
            }
            
            log.info("✅ XML 파싱 완료! 총 {}개 아이템 (노드: {}개)", items.size(), itemNodes.getLength());
            
        } catch (Exception e) {
            log.error("❌ XML 파싱 오류: {}", e.getMessage(), e);
            log.error("   XML 응답 일부: {}", xmlResponse != null && xmlResponse.length() > 500 
                ? xmlResponse.substring(0, 500) + "..." : xmlResponse);
        }
        
        return items;
    }
    
    private Item parseItemElement(Element itemElement) {
        try {
            Item item = new Item();
            
            // 기본 식별 정보
            item.setRnum(getElementText(itemElement, "RNUM"));
            item.setPlnmNo(getElementText(itemElement, "PLNM_NO"));
            item.setPbctNo(getElementText(itemElement, "PBCT_NO"));
            item.setOrgBaseNo(getElementText(itemElement, "ORG_BASE_NO"));
            item.setOrgNm(getElementText(itemElement, "ORG_NM"));
            item.setPbctCdtnNo(getElementText(itemElement, "PBCT_CDTN_NO"));
            item.setCltrNo(getElementText(itemElement, "CLTR_NO"));
            item.setCltrMnmtNo(getElementText(itemElement, "CLTR_MNMT_NO"));
            item.setScrnGrpCd(getElementText(itemElement, "SCRN_GRP_CD"));
            item.setCtgrId(getElementText(itemElement, "CTGR_ID"));
            item.setCtgrFullNm(getElementText(itemElement, "CTGR_FULL_NM"));
            item.setBidMnmtNo(getElementText(itemElement, "BID_MNMT_NO"));
            item.setCltrHstrNo(getElementText(itemElement, "CLTR_HSTR_NO"));
            
            // 물건 정보
            item.setCltrNm(getElementText(itemElement, "CLTR_NM"));
            item.setGoodsNm(getElementText(itemElement, "GOODS_NM"));
            item.setManf(getElementText(itemElement, "MANF"));
            
            // 주소 정보
            item.setLdnmAdrs(getElementText(itemElement, "LDNM_ADRS"));
            item.setNmrdAdrs(getElementText(itemElement, "NMRD_ADRS"));
            item.setRodNm(getElementText(itemElement, "ROD_NM"));
            item.setBldNo(getElementText(itemElement, "BLD_NO"));
            
            // 처분/입찰 방식
            item.setDpslMtdCd(getElementText(itemElement, "DPSL_MTD_CD"));
            item.setDpslMtdNm(getElementText(itemElement, "DPSL_MTD_NM"));
            item.setBidMtdNm(getElementText(itemElement, "BID_MTD_NM"));
            
            // 가격 정보 (원본 값 로깅)
            String minBidPrcText = getElementText(itemElement, "MIN_BID_PRC");
            String apslAsesAvgAmtText = getElementText(itemElement, "APSL_ASES_AVG_AMT");
            
            // 마이너스 값이 있는지 확인하고 로깅
            if (minBidPrcText != null && minBidPrcText.contains("-")) {
                log.warn("⚠️ API에서 마이너스 최저입찰가 발견! CLTR_NO: {}, 원본값: {}", 
                    getElementText(itemElement, "CLTR_NO"), minBidPrcText);
            }
            if (apslAsesAvgAmtText != null && apslAsesAvgAmtText.contains("-")) {
                log.warn("⚠️ API에서 마이너스 감정평가액 발견! CLTR_NO: {}, 원본값: {}", 
                    getElementText(itemElement, "CLTR_NO"), apslAsesAvgAmtText);
            }
            
            item.setMinBidPrc(getElementLong(itemElement, "MIN_BID_PRC"));
            item.setApslAsesAvgAmt(getElementLong(itemElement, "APSL_ASES_AVG_AMT"));
            item.setFeeRate(getElementText(itemElement, "FEE_RATE"));
            
            // 입찰 일정
            item.setPbctBegnDtm(getElementText(itemElement, "PBCT_BEGN_DTM"));
            item.setPbctClsDtm(getElementText(itemElement, "PBCT_CLS_DTM"));
            
            // 상태 및 통계
            item.setPbctCltrStatNm(getElementText(itemElement, "PBCT_CLTR_STAT_NM"));
            item.setUscbCnt(getElementInteger(itemElement, "USCBD_CNT"));
            item.setIqryCnt(getElementInteger(itemElement, "IQRY_CNT"));
            
            return item;
            
        } catch (Exception e) {
            log.error("❌ Item Element 파싱 오류: {}", e.getMessage());
            return null;
        }
    }
    private String getElementText(Element parent, String tagName) {
        try {
            NodeList nodeList = parent.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        } catch (Exception e) {
            // 무시
        }
        return null;
    }
    
    private Long getElementLong(Element parent, String tagName) {
        String text = getElementText(parent, tagName);
        if (text == null || text.trim().isEmpty()) return null;
        try {
            // 마이너스 기호와 숫자만 유지 (쉼표, 공백 등 제거)
            String cleaned = text.replaceAll("[^0-9-]", "");
            if (cleaned.isEmpty()) return null;
            // 마이너스 값이면 null 반환 (가격은 양수여야 함)
            if (cleaned.startsWith("-")) {
                log.warn("Negative price detected for {}: {}", tagName, text);
                return null;
            }
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long value for {}: {}", tagName, text, e);
            return null;
        }
    }
    
    private Integer getElementInteger(Element parent, String tagName) {
        String text = getElementText(parent, tagName);
        if (text == null || text.trim().isEmpty()) return null;
        try {
            String cleaned = text.replaceAll("[^0-9]", "");
            if (cleaned.isEmpty()) return null;
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    // =============================================================================
    // ApiService 통합 메서드
    // =============================================================================
    
    /**
     * API에서 물건 목록 조회 (ApiService의 fetchItemsFromApi 통합)
     */
    public List<Item> fetchItemsFromApi() {
        return fetchItemsFromApi(1, 2000);
    }
    
    /**
     * API에서 물건 목록 조회 (ApiService의 fetchItemsFromApi 통합)
     */
    @org.springframework.cache.annotation.Cacheable(value = "apiItems", key = "#pageNo + '_' + #numOfRows")
    public List<Item> fetchItemsFromApi(int pageNo, int numOfRows) {
        log.info("🔄 API 호출 중... (타임아웃: 5초)");
        
        try {
            // SIDO 파라미터 추가 (서울특별시)
            String sido = URLEncoder.encode("서울특별시", StandardCharsets.UTF_8);
            String url = getKamcoApiUrl() +
                    "?serviceKey=" + getKamcoApiKey() +
                    "&numOfRows=" + numOfRows +
                    "&pageNo=" + pageNo +
                    "&SIDO=" + sido;
            
            // XML 응답 받기
            String xmlResponse = restTemplate.getForObject(new URI(url), String.class);
            
            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.warn("⚠️ API 응답이 비어있습니다. 빈 리스트 반환.");
                return new ArrayList<>();
            }
            
            // XML 파싱 (서울특별시 필터링 포함)
            List<Item> items = parseXmlResponseWithSidoFilter(xmlResponse);
            
            if (items.isEmpty()) {
                log.warn("⚠️ 파싱된 아이템이 없습니다. 빈 리스트 반환.");
                return new ArrayList<>();
            }
            
            log.info("✅ API 호출 성공! {}개 아이템 로드", items.size());
            log.info("📍 지역: 서울특별시, 요청: {}개, 실제: {}개", numOfRows, items.size());
            return items;

        } catch (Exception e) {
            log.error("⚠️ API 호출 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("❌ 빈 리스트 반환 (더미 데이터 없음)");
            return new ArrayList<>();
        }
    }
    
    /**
     * XML 응답을 파싱하여 Item 리스트로 변환 (서울특별시 필터링 포함)
     */
    private List<Item> parseXmlResponseWithSidoFilter(String xmlResponse) {
        List<Item> items = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));
            
            // <item> 태그 찾기
            NodeList itemNodes = doc.getElementsByTagName("item");
            log.info("✅ item 노드 개수: {}", itemNodes.getLength());
            
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element itemElement = (Element) itemNodes.item(i);
                
                // 주소에서 시도 추출하여 서울특별시만 필터링
                String ldnmAdrs = getElementText(itemElement, "LDNM_ADRS");
                String sido = extractSidoFromAddress(ldnmAdrs);
                
                // 서울특별시가 아니면 스킵
                if (sido == null || !sido.equals("서울특별시")) {
                    continue;
                }
                
                Item item = parseItemElement(itemElement);
                if (item != null) {
                    items.add(item);
                }
            }
            
            log.info("✅ XML 파싱 완료! 서울특별시 {}개 아이템", items.size());
            
        } catch (Exception e) {
            log.error("❌ XML 파싱 오류: {}", e.getMessage(), e);
        }
        
        return items;
    }
    
    /**
     * 주소에서 시도 추출 (ApiService의 extractSido 통합)
     */
    private String extractSidoFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        
        // 서울특별시, 부산광역시 등 시도명 추출
        if (address.startsWith("서울특별시")) {
            return "서울특별시";
        } else if (address.startsWith("부산광역시")) {
            return "부산광역시";
        } else if (address.startsWith("대구광역시")) {
            return "대구광역시";
        } else if (address.startsWith("인천광역시")) {
            return "인천광역시";
        } else if (address.startsWith("광주광역시")) {
            return "광주광역시";
        } else if (address.startsWith("대전광역시")) {
            return "대전광역시";
        } else if (address.startsWith("울산광역시")) {
            return "울산광역시";
        } else if (address.startsWith("세종특별자치시")) {
            return "세종특별자치시";
        } else if (address.startsWith("경기도")) {
            return "경기도";
        } else if (address.startsWith("강원")) {
            return "강원도";
        } else if (address.startsWith("충청북도") || address.startsWith("충북")) {
            return "충청북도";
        } else if (address.startsWith("충청남도") || address.startsWith("충남")) {
            return "충청남도";
        } else if (address.startsWith("전라북도") || address.startsWith("전북") || address.startsWith("전북특별자치도")) {
            return "전북특별자치도";
        } else if (address.startsWith("전라남도") || address.startsWith("전남")) {
            return "전라남도";
        } else if (address.startsWith("경상북도") || address.startsWith("경북")) {
            return "경상북도";
        } else if (address.startsWith("경상남도") || address.startsWith("경남")) {
            return "경상남도";
        } else if (address.startsWith("제주")) {
            return "제주특별자치도";
        }
        
        // 앞 3글자로 추정
        if (address.length() >= 3) {
            return address.substring(0, 3);
        }
        
        return address;
    }
}



