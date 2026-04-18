package com.uet.bidding.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Button;

public class AuctionListController {

    // Khai báo các thành phần giao diện
    @FXML private TableView<?> tableAuctions;
    @FXML private TableColumn<?, ?> colName;
    @FXML private TableColumn<?, ?> colPrice;
    @FXML private TableColumn<?, ?> colStatus;

    @FXML private Button btnJoin;

    // Hàm này chạy ngay khi màn hình vừa được bật lên
    @FXML
    public void initialize() {
        System.out.println("Màn hình danh sách đã sẵn sàng!");
        // TODO: Chúng ta sẽ nạp danh sách sản phẩm vào bảng ở đây
    }

    // Hàm xử lý khi bấm nút "Vào phòng đấu giá"
    @FXML
    public void handleJoinAuction() {
        System.out.println("Chuẩn bị chuyển sang màn hình đấu giá chi tiết...");
    }
}