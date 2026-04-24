package com.uet.bidding.ui;

import com.uet.bidding.model.AuctionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent; // Bắt buộc phải import thư viện này cho nút bấm
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

    // Khai báo các ID phải trùng khớp với fx:id trong AuctionList.fxml
    @FXML private TableView<AuctionItem> tableView;
    @FXML private TableColumn<AuctionItem, Integer> colStt;
    @FXML private TableColumn<AuctionItem, String> colCity;
    @FXML private TableColumn<AuctionItem, String> colProduct;
    @FXML private TableColumn<AuctionItem, Integer> colInterested;
    @FXML private TableColumn<AuctionItem, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Cấu hình để các cột biết lấy dữ liệu từ đâu trong AuctionItem
        colStt.setCellValueFactory(new PropertyValueFactory<>("stt"));
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productType"));
        colInterested.setCellValueFactory(new PropertyValueFactory<>("interestedCount"));

        // 2. Tạo nút "Đăng kí đấu giá" cho cột Lựa chọn
        setupActionColumn();

        // 3. TẠO DỮ LIỆU MẪU ĐỂ LẤP ĐẦY BẢNG
        ObservableList<AuctionItem> dataList = FXCollections.observableArrayList(
                new AuctionItem(1, "Hà Nội", "Laptop Dell XPS 15", 125),
                new AuctionItem(2, "Đà Nẵng", "Đồng hồ Apple Watch S9", 45),
                new AuctionItem(3, "TP. HCM", "Xe đạp điện VinFast", 89),
                new AuctionItem(4, "Cần Thơ", "Máy ảnh Canon EOS R5", 12)
        );

        // 4. Đưa dữ liệu vào TableView
        tableView.setItems(dataList);
    }

    private void setupActionColumn() {
        Callback<TableColumn<AuctionItem, Void>, TableCell<AuctionItem, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<AuctionItem, Void> call(final TableColumn<AuctionItem, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Đăng kí đấu giá");
                    {
                        // Bo góc và tô màu cho giống bản vẽ của bạn
                        btn.setStyle("-fx-background-color: white; -fx-border-color: black; " +
                                "-fx-border-radius: 20; -fx-background-radius: 20; " +
                                "-fx-text-fill: #e84393; -fx-font-weight: bold; -fx-cursor: hand;");
                        btn.setOnAction(event -> {
                            AuctionItem item = getTableView().getItems().get(getIndex());
                            System.out.println("Đăng kí sản phẩm: " + item.getProductType());
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    // THÊM HÀM NÀY ĐỂ TRÁNH LỖI FXML TÌM KHÔNG THẤY HÀM XỬ LÝ NÚT TÌM KIẾM
    @FXML
    public void handleSearch(ActionEvent event) {
        System.out.println("Nút tìm kiếm vừa được bấm!");
        // (Sau này bạn có thể code chức năng lọc dữ liệu theo tên thành phố hoặc sản phẩm ở đây)
    }
}