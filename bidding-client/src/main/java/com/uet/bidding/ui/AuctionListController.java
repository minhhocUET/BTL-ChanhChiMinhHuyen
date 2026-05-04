package com.uet.bidding.ui;

import com.uet.bidding.model.AuctionItem; // Gọi class AuctionItem của bạn vào
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AuctionListController {

    // Khai báo bảng và các cột, chỉ định rõ nó dùng dữ liệu từ AuctionItem
    @FXML
    private TableView<AuctionItem> auctionTable;
    @FXML
    private TableColumn<AuctionItem, String> nameColumn;
    @FXML
    private TableColumn<AuctionItem, Double> priceColumn;
    @FXML
    private TableColumn<AuctionItem, String> timeColumn;

    @FXML
    public void initialize() {
        // 1. Liên kết các cột với các biến (name, currentPrice, timeLeft) trong file AuctionItem.java
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeLeft"));

        // 2. Tạo một ít dữ liệu mẫu để đưa lên giao diện
        ObservableList<AuctionItem> dummyData = FXCollections.observableArrayList(
                new AuctionItem("Biển số: 30K-999.99", 5000.0, "00:45:12 (Đang chạy)"),
                new AuctionItem("Laptop Dell XPS 15", 1200.0, "Sắp bắt đầu"),
                new AuctionItem("Tranh sơn dầu Thế kỷ 19", 8500.0, "02:10:05 (Đang chạy)")
        );

        // 3. Đổ dữ liệu vào bảng
        auctionTable.setItems(dummyData);

        System.out.println("🟢 Đã tải xong giao diện Đấu giá và đưa dữ liệu mẫu lên bảng!");
    }
}