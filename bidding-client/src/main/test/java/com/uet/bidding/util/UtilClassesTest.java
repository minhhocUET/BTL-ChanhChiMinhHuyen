package com.uet.bidding.util;

import com.uet.bidding.model.Admin;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.Item;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilClassesTest {

  @BeforeAll
  static void initJavaFX() {
    // Chỉ chạy 1 lần duy nhất cho toàn bộ class test
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException e) {
      // Toolkit đã được khởi tạo rồi (không sao cả)
    }
  }

  // ==========================================
  // 1. TEST CHO USERSESSION
  // ==========================================
  @Nested
  class UserSessionTests {
    @BeforeEach
    void setUp() {
      UserSession.clear();
    }

    @Test
    void testSessionWithCustomer() {
      Customer mockCustomer = mock(Customer.class);
      UserSession.setCurrentUser(mockCustomer);

      assertTrue(UserSession.isLoggedIn());
      assertEquals(mockCustomer, UserSession.getCurrentUser());
      assertEquals(mockCustomer, UserSession.getLoggedInCustomer());
      assertNull(UserSession.getLoggedInAdmin());
    }

    @Test
    void testSessionWithAdmin() {
      Admin mockAdmin = mock(Admin.class);
      UserSession.setCurrentUser(mockAdmin);

      assertTrue(UserSession.isLoggedIn());
      assertEquals(mockAdmin, UserSession.getCurrentUser());
      assertEquals(mockAdmin, UserSession.getLoggedInAdmin());
      assertNull(UserSession.getLoggedInCustomer());
    }

    @Test
    void testClearSession() {
      UserSession.setCurrentUser(mock(Customer.class));
      UserSession.clear();

      assertFalse(UserSession.isLoggedIn());
      assertNull(UserSession.getCurrentUser());
    }
  }

  // ==========================================
  // 2. TEST CHO CONTEXTS (Lưu trạng thái)
  // ==========================================
  @Nested
  class ContextTests {

    @Test
    void testReviewContext() {
      // Test hàm set full tham số
      ReviewContext.set(10, 20, "Shop ABC", true, "http://avatar.com/1.png");
      assertEquals(10, ReviewContext.auctionId);
      assertEquals(20, ReviewContext.sellerId);
      assertEquals("Shop ABC", ReviewContext.storeName);
      assertTrue(ReviewContext.showAddForm);
      assertEquals("http://avatar.com/1.png", ReviewContext.avatarUrl);

      // Test hàm set khuyết tham số (avatarUrl null)
      ReviewContext.set(99, 88, "Shop XYZ", false);
      assertNull(ReviewContext.avatarUrl);
      assertFalse(ReviewContext.showAddForm);
    }

    @Test
    void testSellerAuctionContext() {
      Auction mockAuction = mock(Auction.class);
      SellerAuctionContext.set(mockAuction);
      assertEquals(mockAuction, SellerAuctionContext.get());

      SellerAuctionContext.clear();
      assertNull(SellerAuctionContext.get());
    }

    @Test
    void testCreateAuctionContext() {
      Item mockItem = mock(Item.class);
      CreateAuctionContext.set(mockItem);
      assertEquals(mockItem, CreateAuctionContext.get());

      CreateAuctionContext.clear();
      assertNull(CreateAuctionContext.get());
    }
  }

  // ==========================================
  // 3. TEST CHO IMAGE UTILS
  // ==========================================
  @Nested
  class ImageUtilsTests {

    private ImageView imageView;
    private Label label;

    @BeforeEach
    void setUp() {
      // KHÔNG MOCK CLASS JAVA FX NỮA
      // Khởi tạo đối tượng thật, chúng sẽ không lỗi khi chạy trên luồng main
      imageView = new ImageView();
      label = new Label();
    }

    @Test
    void testLoadAvatarFromUrl_NullOrBlank() {
      ImageUtils.loadAvatarFromUrl(imageView, label, null);

      assertFalse(imageView.isVisible());
      assertTrue(label.isVisible());
    }

    @Test
    void testLoadAvatarFromUrl_CloudinarySuccess() {
      // KHÔNG dùng mockConstruction nữa
      String cloudUrl = "https://cloudinary.com/upload/test.jpg";

      // Gọi hàm test
      ImageUtils.loadAvatarFromUrl(imageView, label, cloudUrl);

      // Kiểm tra
      // Lúc này imageView.getImage() sẽ trả về một Image thật
      assertNotNull(imageView.getImage());
      assertTrue(imageView.isVisible());
      assertFalse(label.isVisible());
    }

    @Test
    void testLoadItemImage_NullChecks() {
      // Item thật hoặc Mock đều được, nhưng ImageView phải là thật
      ImageUtils.loadItemImage(imageView, null);
      assertNull(imageView.getImage());
    }

    @Test
    void testLoadAvatarFromUrl_LocalFileSuccess() throws Exception {
      // Tạo file tạm để giả lập file cục bộ
      File tempFile = File.createTempFile("test_img", ".jpg");

      ImageUtils.loadAvatarFromUrl(imageView, label, tempFile.getAbsolutePath());

      assertTrue(imageView.isVisible());
      assertFalse(label.isVisible());
      assertNotNull(imageView.getImage());

      tempFile.delete(); // Dọn dẹp
    }

    @Test
    void testLoadAvatarFromUrl_InvalidUrlFallsBackToLabel() {
      // URL không phải là http và cũng không phải là file tồn tại
      ImageUtils.loadAvatarFromUrl(imageView, label, "invalid_path_xyz");

      assertFalse(imageView.isVisible());
      assertTrue(label.isVisible());
      assertNull(imageView.getImage());
    }

    @Test
    void testLoadItemImage_LocalFileSuccess() throws Exception {
      File tempFile = File.createTempFile("item_img", ".png");
      Item mockItem = mock(Item.class);
      when(mockItem.getImagePath()).thenReturn(tempFile.getAbsolutePath());

      ImageUtils.loadItemImage(imageView, mockItem);

      assertNotNull(imageView.getImage());

      tempFile.delete();
    }

    @Test
    void testLoadItemImage_CloudinaryTransformationApplied() {
      // Test logic thay thế "/upload/" -> "/upload/a_auto/"
      String originalUrl = "https://res.cloudinary.com/demo/image/upload/v123/test.jpg";
      Item mockItem = mock(Item.class);
      when(mockItem.getImagePath()).thenReturn(originalUrl);

      ImageUtils.loadItemImage(imageView, mockItem);

      // Kiểm tra xem URL đã được thay đổi trong đối tượng Image chưa
      // Image constructor trong JavaFX lấy URL từ thuộc tính source
      String loadedUrl = imageView.getImage().getUrl();
      assertTrue(loadedUrl.contains("/upload/a_auto/"));
      assertFalse(loadedUrl.contains("/upload/v123")); // đã bị thay thế
    }

    @Test
    void testLoadItemImage_BlankPath() {
      Item mockItem = mock(Item.class);
      when(mockItem.getImagePath()).thenReturn("   "); // Khoảng trắng

      ImageUtils.loadItemImage(imageView, mockItem);

      assertNull(imageView.getImage());
    }

    @Test
    void testLoadAvatarFromUrl_EmptyUrl() {
      // Test trường hợp chuỗi rỗng
      ImageUtils.loadAvatarFromUrl(imageView, label, "");
      assertFalse(imageView.isVisible());
      assertTrue(label.isVisible());
    }

    @Test
    void testLoadAvatarFromUrl_NullView() {
      // Test trường hợp view truyền vào là null (phải thoát hàm an toàn)
      assertDoesNotThrow(() -> ImageUtils.loadAvatarFromUrl(null, label, "http://test.com/a.jpg"));
    }

    @Test
    void testLoadItemImage_InvalidLocalPath() {
      // Test load ảnh từ ổ đĩa nhưng file không tồn tại
      Item mockItem = mock(Item.class);
      when(mockItem.getImagePath()).thenReturn("C:/path/to/non_existent_file.jpg");

      ImageUtils.loadItemImage(imageView, mockItem);

      // Mong đợi ảnh là null vì file không tồn tại
      assertNull(imageView.getImage());
    }

    @Test
    void testLoadItemImage_TransformationCheck() {
      // Kiểm tra xem đoạn replace "/upload/" sang "/upload/a_auto/" có chạy không
      String original = "https://res.cloudinary.com/demo/image/upload/v123/test.jpg";
      Item mockItem = mock(Item.class);
      when(mockItem.getImagePath()).thenReturn(original);

      ImageUtils.loadItemImage(imageView, mockItem);

      // Kiểm tra URL được gán vào ImageView
      // Nếu load thành công, image.getUrl() sẽ chứa "/upload/a_auto/"
      assertNotNull(imageView.getImage());
      assertTrue(imageView.getImage().getUrl().contains("/upload/a_auto/"));
    }
  }


}