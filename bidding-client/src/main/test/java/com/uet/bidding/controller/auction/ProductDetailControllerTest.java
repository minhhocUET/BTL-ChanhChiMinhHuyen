package com.uet.bidding.controller.auction;

import com.uet.bidding.controller.auction.ProductDetailController;
import com.uet.bidding.model.*;
import com.uet.bidding.service.AutoBidService;
import com.uet.bidding.service.BidderService;
import com.uet.bidding.util.UserSession;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import javafx.embed.swing.JFXPanel;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProductDetailControllerTest {

  private ProductDetailController controller;
  private Auction auction;
  private Customer customer;

  @BeforeAll
  static void initToolkit() {
    // Khởi tạo JavaFX Toolkit một lần cho toàn bộ test
    new JFXPanel(); // Tạo panel giả để kích hoạt JavaFX runtime
    Platform.setImplicitExit(false);
  }

  private void setPrivateField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Object getPrivateField(Object target, String fieldName) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void invokePrivateMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      method.invoke(target, args);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }



  @BeforeEach
  void setup() {
    controller = new ProductDetailController();

    // Fake UI controls
    setPrivateField(controller, "lblProductName", new Label());
    setPrivateField(controller, "lblAuctionId", new Label());
    setPrivateField(controller, "lblCurrentPrice", new Label());
    setPrivateField(controller, "lblHighestBidder", new Label());
    setPrivateField(controller, "lblCountdown", new Label());
    setPrivateField(controller, "lblDescription", new Label());
    setPrivateField(controller, "txtBidAmount", new TextField());
    setPrivateField(controller, "lblMinBid", new Label());
    setPrivateField(controller, "lblAntiSnipeInfo", new Label());
    setPrivateField(controller, "txtMaxAutoBid", new TextField());
    setPrivateField(controller, "btnEnableAutoBid", new Button());
    setPrivateField(controller, "btnSellerReviews", new Button());
    setPrivateField(controller, "btnRegister", new Button());
    setPrivateField(controller, "lblRegisterHint", new Label());
    setPrivateField(controller, "lblRegisteredCount", new Label());
    setPrivateField(controller, "lblWalletBalance", new Label());
    setPrivateField(controller, "imgProduct", new ImageView());


    // Fake auction + item
    Item item = mock(Item.class);
    when(item.getName()).thenReturn("Laptop");
    when(item.getDescription()).thenReturn("Gaming laptop");
    when(item.getSellerId()).thenReturn(99);

    auction = new Auction();
    auction.setId(1);
    auction.setItem(item);
    auction.setCurrentPrice(BigDecimal.valueOf(1000));
    auction.setBidIncrement(BigDecimal.valueOf(100));
    auction.setEndTime(LocalDateTime.now().plusSeconds(2));
    auction.setRegisteredCount(0);

    // Khởi tạo Customer
    customer = new Customer();
    customer.setId(1);
    customer.setUsername("tester");
    customer.setBalance(BigDecimal.valueOf(5000));
    customer.setFullName("Nguyen Van A");
    customer.setPhone("0123456789");
    customer.setAddress("Hanoi");

    // Đặt customer vào session
    UserSession.setCurrentUser(customer);
  }

  @Test
  void testSetAuctionData() {
    controller.setAuctionData(auction);
    Label lblProductName = (Label) getPrivateField(controller, "lblProductName");
    Label lblCurrentPrice = (Label) getPrivateField(controller, "lblCurrentPrice");

    assertEquals("Laptop", lblProductName.getText());
    assertTrue(lblCurrentPrice.getText().contains("VNĐ"));
    assertNotNull(ProductDetailController.getInstance());
  }

  @Test
  void testRefreshWalletBalanceLabel() {
    controller.setAuctionData(auction);
    controller.refreshWalletBalanceLabel();
    Label lblWalletBalance = (Label) getPrivateField(controller, "lblWalletBalance");
    assertTrue(lblWalletBalance.getText().contains("VNĐ"));
  }

  @Test
  void testHandleEnableAutoBid_InvalidInput() throws InterruptedException {
    controller.setAuctionData(auction);
    TextField txtMaxAutoBid = (TextField) getPrivateField(controller, "txtMaxAutoBid");
    txtMaxAutoBid.setText("");

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      controller.handleEnableAutoBid(); // chạy trên FX thread
      latch.countDown();
    });
    latch.await(); // chờ cho đến khi FX thread xử lý xong
  }


  @Test
  void testHandleEnableAutoBid_ExceedBalance() throws InterruptedException {
    controller.setAuctionData(auction);
    TextField txtMaxAutoBid = (TextField) getPrivateField(controller, "txtMaxAutoBid");
    txtMaxAutoBid.setText("999999");

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      controller.handleEnableAutoBid(); // chạy trên FX thread
      latch.countDown();
    });
    latch.await(); // chờ cho đến khi FX thread xử lý xong
  }


  @Test
  void testHandlePlaceBid_InvalidAmount() throws InterruptedException {
    controller.setAuctionData(auction);
    TextField txtBidAmount = (TextField) getPrivateField(controller, "txtBidAmount");
    txtBidAmount.setText("abc");

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      controller.handlePlaceBid(); // chạy trên FX thread
      latch.countDown();
    });
    latch.await(); // chờ cho đến khi FX thread xử lý xong
  }


  @Test
  void testHandlePlaceBid_TooLow() throws InterruptedException {
    controller.setAuctionData(auction);
    TextField txtBidAmount = (TextField) getPrivateField(controller, "txtBidAmount");
    txtBidAmount.setText("1000"); // must be >= 1100

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      controller.handlePlaceBid(); // chạy trên FX thread
      latch.countDown();
    });
    latch.await(); // chờ cho đến khi FX thread xử lý xong
  }


  @Test
  void testHandlePlaceBid_ExceedBalance() throws InterruptedException {
    controller.setAuctionData(auction);

    // Giảm số dư xuống 100
    customer.setBalance(BigDecimal.valueOf(100));

    TextField txtBidAmount = (TextField) getPrivateField(controller, "txtBidAmount");
    txtBidAmount.setText("1000");

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      controller.handlePlaceBid(); // chạy trên FX thread
      latch.countDown();
    });
    latch.await();
  }


  @Test
  void testCountdownEnds() throws InterruptedException {
    auction.setEndTime(LocalDateTime.now().plusSeconds(1));
    controller.setAuctionData(auction);
    Thread.sleep(1500); // wait for countdown
    Label lblCountdown = (Label) getPrivateField(controller, "lblCountdown");
    assertEquals("ĐÃ KẾT THÚC", lblCountdown.getText());
  }

  @Test
  void testApplyRegistrationButtonState() {
    controller.setAuctionData(auction);
    invokePrivateMethod(controller, "applyRegistrationButtonState", new Class[]{boolean.class}, true);
    Button btnRegister = (Button) getPrivateField(controller, "btnRegister");
    Label lblRegisterHint = (Label) getPrivateField(controller, "lblRegisterHint");

    assertTrue(btnRegister.isDisabled());
    assertTrue(lblRegisterHint.getText().contains("đã đăng ký"));

    invokePrivateMethod(controller, "applyRegistrationButtonState", new Class[]{boolean.class}, false);
    assertFalse(btnRegister.isDisabled());
  }

  @Test
  void testHandleRegister_SellerCannotRegister() throws InterruptedException {
    customer.setId(99); // same as seller
    controller.setAuctionData(auction);

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      controller.handleRegister(); // chạy trên FX thread
      latch.countDown();
    });
    latch.await(); // chờ cho đến khi FX thread xử lý xong
  }


  @Test
  void testUpdateMinBidLabel() {
    controller.setAuctionData(auction);
    Label lblMinBid = (Label) getPrivateField(controller, "lblMinBid");
    assertTrue(lblMinBid.getText().contains("Tối thiểu"));
  }
}
