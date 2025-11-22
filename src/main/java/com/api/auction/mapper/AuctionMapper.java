package com.api.auction.mapper;

import java.sql.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.api.auction.domain.Auction;
import com.api.auction.domain.FindBoard;
import com.api.auction.domain.Reply;

@Mapper
public interface AuctionMapper {

	void insertAuction(Auction at);
	
	List<Auction> getAuctionList(@Param("printType") String printType, 
								 @Param("period") String period, 
								 @Param("keyword") String keyword, 
								 @Param("startPage") int startPage, 
								 @Param("num") int num);
	
	Auction getAuction(int no);
	
	void updateEndPrice(@Param("no") int no, 
					    @Param("buyer") String buyer, 
					    @Param("endPrice") int endPrice);
	
	void updateAuction(Auction auction);
	
	void deleteAuction(int no);

	void insertBoard(FindBoard board);
	
	List<FindBoard> getBoardList(@Param("id") String id, 
	                             @Param("keyword") String keyword,
	                             @Param("category") String category);

	FindBoard getBoard(int no);

	void updateBoard(FindBoard board);
	
	void incrementBoardViews(int no);
	
	void deleteBoard(int no);
	
	void insertReply(Reply re);
	
	void updateReply(Reply re);
	
	void deleteReply(int no);
	
	List<Reply> getReplyList(int boardNo);
	
	List<Auction> getMyAuctionList(@Param("id") String id, @Param("option") String option);
	
	int getAuctionCount(@Param("period") String period, @Param("keyword") String keyword);
	
	List<Auction> getCommissionList(@Param("start") Date start, @Param("end") Date end);
}

