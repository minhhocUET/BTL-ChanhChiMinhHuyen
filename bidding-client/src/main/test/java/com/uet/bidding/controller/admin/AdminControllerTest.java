package com.uet.bidding.controller.admin;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class AdminControllerTest {

  private AdminController controller;
  private StackPane mainContent;

  @Start
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Admin.fxml")); // Hoặc tên file FXML tương ứng của bạn
    Parent root = loader.load();
    controller = loader.getController();

    mainContent = (StackPane) root.lookup("#mainContent");

    stage.setScene(new Scene(root));
    stage.show();
  }

  @Test
  public void testInitialize_MặcĐịnhHiểnThịTrangUser() {
    WaitForAsyncUtils.waitForFxEvents();

    // THEN: Theo thiết kế hàm initialize(), vừa bật app lên màn hình con phải được lấp đầy luôn
    assertNotNull(mainContent.getChildren().get(0), "Khi khởi tạo, tab Quản lý User phải được nạp mặc định");
  }
}