package com.uet.bidding.model;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class AuctionItem {
    private final SimpleIntegerProperty stt;
    private final SimpleStringProperty city;
    private final SimpleStringProperty productType;
    private final SimpleIntegerProperty interestedCount;

    public AuctionItem(int stt, String city, String productType, int interestedCount) {
        this.stt = new SimpleIntegerProperty(stt);
        this.city = new SimpleStringProperty(city);
        this.productType = new SimpleStringProperty(productType);
        this.interestedCount = new SimpleIntegerProperty(interestedCount);
    }

    public int getStt() { return stt.get(); }
    public String getCity() { return city.get(); }
    public String getProductType() { return productType.get(); }
    public int getInterestedCount() { return interestedCount.get(); }
}