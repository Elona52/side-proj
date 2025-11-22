package com.api.common.dto;

import java.util.ArrayList;
import java.util.List;

import com.api.item.domain.Item;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response") // XML 최상위 태그
public class ApiResponse {

    private Body body;

    @XmlElement(name = "body")
    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public List<Item> toItems() {
        List<Item> itemsList = new ArrayList<>();
        if (body != null && body.getItems() != null) {
            for (Item Item : body.getItems().getItemList()) {
                Item item = new Item();
                item.setCltrNm(Item.getCltrNm());
                item.setDpslMtdCd(Item.getDpslMtdCd());
                item.setLdnmAdrs(Item.getLdnmAdrs());
                item.setMinBidPrc(Item.getMinBidPrc());
                item.setApslAsesAvgAmt(Item.getApslAsesAvgAmt());
                item.setPbctBegnDtm(Item.getPbctBegnDtm());
                item.setPbctClsDtm(Item.getPbctClsDtm());
                itemsList.add(item);
            }
        }
        return itemsList;
    }

    public static class Body {
        private Items items;

        @XmlElement(name = "items")
        public Items getItems() {
            return items;
        }

        public void setItems(Items items) {
            this.items = items;
        }
    }

    public static class Items {
        private List<Item> itemList;

        @XmlElement(name = "item")
        public List<Item> getItemList() {
            return itemList;
        }

        public void setItemList(List<Item> itemList) {
            this.itemList = itemList;
        }
    }
}


