# 데이터베이스 ERD (Entity Relationship Diagram)

## 📊 전체 ERD

ERD 다이어그램은 별도 파일로 분리되었습니다: [DATABASE_ERD.mmd](./DATABASE_ERD.mmd)

Mermaid 다이어그램을 보려면:
- GitHub에서 자동으로 렌더링됩니다
- VS Code에서 Mermaid 확장 프로그램을 설치하면 미리보기 가능합니다
- 온라인 Mermaid 에디터에서 열어볼 수 있습니다: https://mermaid.live/

## 📋 테이블 목록

### 1. 회원 관련
- **KNMember**: 회원 정보

### 2. 경매 관련
- **KNAuction**: 경매 정보
- **KNPayment**: 결제 정보 (경매 또는 API 물건)
- **KNPaymentHistory**: 결제 히스토리

### 3. 게시판 관련
- **KNfind**: 게시판
- **KNReply**: 댓글

### 4. 공매 물건 관련
- **KNKamcoItem**: 캠코 온비드 공매 물건
- **KNKamcoItemViewLog**: 물건 조회 이력
- **KNNewItemNotification**: 신규 물건 공지
- **KNKamcoItemStats**: 물건 통계 스냅샷

### 5. 즐겨찾기 관련
- **User_Favorite**: 사용자 즐겨찾기
- **KNPriceAlert**: 가격 알림 히스토리

## 🔗 주요 관계

### 1:N 관계
- `KNMember` → `KNAuction` (판매자, 낙찰자)
- `KNMember` → `KNfind` (작성자)
- `KNMember` → `KNReply` (작성자)
- `KNMember` → `User_Favorite` (회원)
- `KNMember` → `KNPriceAlert` (회원)
- `KNMember` → `KNPayment` (구매자)
- `KNMember` → `KNKamcoItemViewLog` (조회자)
- `KNAuction` → `KNPayment` (경매 - auction_no)
- `KNPayment` → `KNPaymentHistory` (결제)
- `KNfind` → `KNReply` (게시글)
- `KNKamcoItem` → `User_Favorite` (물건)
- `KNKamcoItem` → `KNPriceAlert` (물건)
- `KNKamcoItem` → `KNKamcoItemViewLog` (물건)
- `KNKamcoItem` → `KNNewItemNotification` (물건)
- `KNKamcoItem` → `KNKamcoItemStats` (물건)
- `KNKamcoItem` → `KNPayment` (물건 - item_id/cltr_no)
- `User_Favorite` → `KNPriceAlert` (즐겨찾기)

## 📝 참고사항

### 뷰 (View)
- **KNPublicAuctionInfo**: `KNKamcoItem`의 뷰 (데이터 중복 제거를 위해 뷰로 변경됨)

### 인덱스
- 주요 외래키와 검색에 사용되는 컬럼에 인덱스가 설정되어 있습니다.
- 자세한 인덱스 정보는 각 SQL 파일을 참고하세요.

### 제약조건
- 대부분의 외래키는 `ON DELETE CASCADE`로 설정되어 있습니다.
- `KNAuction.buyer`는 `ON DELETE SET NULL`로 설정되어 있습니다 (낙찰 전에는 NULL 가능).
- `User_Favorite`는 `UNIQUE KEY unique_user_favorite (user_id, item_id)`로 중복 방지.
- `KNKamcoItem.cltr_no`는 `UNIQUE` 제약조건으로 중복 방지.
- `KNPayment.imp_uid`와 `merchant_uid`는 `UNIQUE` 제약조건.
- `KNKamcoItemStats`는 `UNIQUE KEY unique_item_stat (item_id, stat_date)`로 중복 방지.

### 특이사항
- **KNPayment**: 
  - `auction_no`, `item_id`, `cltr_no` 중 하나 이상이 있어야 함
  - DB 경매인 경우: `auction_no`만 설정
  - API 물건인 경우: `auction_no`는 NULL, `item_id`와 `cltr_no` 설정
  - 중복 방지: 같은 회원이 같은 물건에 대해 중복 입찰 방지 로직 구현

### 전체 구조 개요

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           데이터베이스 구조 개요                              │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│  KNMember    │ ◄──┐
│  (회원)       │    │
└──────┬───────┘    │
       │            │
       ├────────────┼──────────────────────────────────────────────┐
       │            │                                              │
       ▼            ▼                                              ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  KNAuction   │  │   KNfind     │  │  KNReply     │  │User_Favorite │
│  (경매)       │  │  (게시판)      │  │  (댓글)       │  │(즐겨찾기)      │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │                 │
       │                 │                 │                 │
       ▼                 ▼                 │                 │
┌──────────────┐  ┌──────────────┐         │                 │
│  KNPayment   │  │  (댓글은       │         │                 │
│  (결제)       │  │   게시글에      │         │                 │
│  -auction_no │  │   속함)        │         │                 │
│  -item_id    │  └───────────────┘         │                 │
│  -cltr_no    │                            │                 │
└──────┬───────┘                            │                 │
       │                                    │                 │
       ▼                                    │                 │
┌──────────────┐                            │                 │
│KNPaymentHist │                            │                 │
│  (결제이력)    │                            │                 │
└──────────────┘                            │                 │
                                            │                 │
┌───────────────────────────────────────────┴─────────────── ──┴──────────┐
│                                                                         │
│                    ┌──────────────────────┐                             │
│                    │   KNKamcoItem        │                             │
│                    │   (공매 물건)          │                             │
│                    └──────────┬───────────┘                             │
│                               │                                         │
│                ┌──────────────┼──────────────┐                          │
│                │              │              │                          │
│                ▼              ▼              ▼                          │
│    ┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐         │
│    │KNKamcoItemViewLog│  │KNNewItemNotif│  │KNKamcoItemStats  │         │
│    │  (조회 이력)       │  │  (신규 공지)    │  │  (통계 스냅샷)     │         │
│    └──────────────────┘  └──────────────┘  └──────────────────┘         │ 
│                                                                         │
│                    ┌──────────────────────┐                             │
│                    │   KNPayment          │ ◄──┐                        │
│                    │   (결제 - item_id)    │     │                       │
│                    └──────────────────────┘     │                       │
│                                                 │                       │
└─────────────────────────────────────────────────┘                       │

                            ┌──────────────┐
                            │KNPriceAlert  │
                            │(가격 알림)     │
                            └──────────────┘
                                  ▲
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
            ┌──────────────┐          ┌──────────────┐
            │User_Favorite │          │  KNMember    │
            └──────────────┘          └──────────────┘
```

### 핵심 관계 다이어그램

```
                    ┌──────────────┐
                    │  KNMember    │ (회원 - 중심 엔티티)
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  KNAuction   │  │   KNfind     │  │User_Favorite │
│  (경매)       │  │  (게시판)      │  │(즐겨찾기)      │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       ▼                 ▼                 │
┌──────────────┐  ┌──────────────┐         │
│  KNPayment   │  │  KNReply     │         │
│  (결제)       │  │  (댓글)       │         │
│  -auction_no │  └──────────────┘         │
│  -item_id    │                           │
│  -cltr_no    │                           │
└──────┬───────┘                           │
       │                                   │
       ▼                                   │
┌──────────────┐                           │
│KNPaymentHist │                           │
│  (결제이력)    │                           │
└──────────────┘                          │
                                          │
                    ┌─────────────────────┘
                    │
                    ▼
            ┌──────────────┐
            │KNKamcoItem   │ (공매 물건 - 중심 엔티티)
            └──────┬───────┘
                   │
    ┌──────────────┼──────────────┐
    │              │              │
    ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│ViewLog   │  │Notif     │  │Stats     │
│(조회이력)  │  │(공지)     │  │(통계)     │
└──────────┘  └──────────┘  └──────────┘
```
