package com.uet.bidding.server;

import com.uet.bidding.dao.AuctionSqlDAO;
import com.uet.bidding.dao.ItemSqlDAO;
import com.uet.bidding.dao.UserSqlDAO;
import com.uet.bidding.model.Auction;
import com.uet.bidding.model.Customer;
import com.uet.bidding.model.NetworkMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestProcessorTest {

  private RequestProcessor requestProcessor;

  @Mock private UserSqlDAO userSqlDAO;
  @Mock private ItemSqlDAO itemSqlDAO;
  @Mock private AuctionSqlDAO auctionSqlDAO;
  @Mock private ClientHandler clientHandler;

  @BeforeEach
  void setUp() {
    requestProcessor = new RequestProcessor(userSqlDAO, itemSqlDAO, auctionSqlDAO);
    com.uet.bidding.service.AuctionManager.getInstance().initialize(auctionSqlDAO);
  }

  @Test
  void testProcessRequest_Login_Success() throws Exception {
    String reqId = "login-req-id";
    // Gửi chuỗi credentials "username password" phân tách bằng dấu cách theo logic server của bạn
    NetworkMessage msg = new NetworkMessage("LOGIN", "testuser 123456");
    msg.setRequestId(reqId);

    Customer fakeCustomer = new Customer("testuser", "hashed_password", BigDecimal.ZERO);
    fakeCustomer.setId(1);

    // Giả lập: Login hợp lệ, không bị ban
    when(userSqlDAO.checkLogin("testuser", "123456")).thenReturn(fakeCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm chứng: Đã lưu trạng thái đăng nhập vào handler và báo SUCCESS về client
    verify(clientHandler, times(1)).setLoggedInUser(fakeCustomer);
    verify(clientHandler, times(1)).sendResponse(eq("LOGIN_SUCCESS"), eq(fakeCustomer), eq(reqId));
  }

  @Test
  void testProcessRequest_Login_Failure_WrongCredentials() throws Exception {
    String reqId = "login-fail-id";
    NetworkMessage msg = new NetworkMessage("LOGIN", "wronguser wrongpass");
    msg.setRequestId(reqId);

    // Giả lập checkLogin trả về null (sai tài khoản/mật khẩu)
    when(userSqlDAO.checkLogin("wronguser", "wrongpass")).thenReturn(null);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm chứng: Phải bắn lỗi ERROR về cho client
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Sai tên đăng nhập hoặc mật khẩu."), eq(reqId));
  }

  @Test
  void testProcessRequest_InvalidCommand_DefaultCase() {
    String reqId = "unknown-id";
    NetworkMessage msg = new NetworkMessage("UNKNOWN_ACTION", "");
    msg.setRequestId(reqId);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm chứng: Rơi vào nhánh default của switch-case
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Lệnh không hợp lệ hoặc chưa được hỗ trợ!"), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM ĐĂNG KÝ TÀI KHOẢN (REGISTER)
  // ==============================================================
  @Test
  void testProcessRequest_Register_Success() {
    String reqId = "reg-123";
    // Định dạng chuỗi đăng ký: "username password" theo hàm handleRegister của bạn
    NetworkMessage msg = new NetworkMessage("REGISTER", "newuser mypassword123");
    msg.setRequestId(reqId);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm chứng: Đã gọi xuống DB để chèn User và báo SUCCESS về client
    verify(userSqlDAO, times(1)).addUser(any(Customer.class));
    verify(clientHandler, times(1)).sendResponse(eq("REGISTER_SUCCESS"), eq("Đăng ký thành công!"), eq(reqId));
  }

  @Test
  void testProcessRequest_Register_Failure_MissingInfo() {
    String reqId = "reg-fail";
    // Gửi thiếu mật khẩu (chỉ gửi username) để kích hoạt UserException
    NetworkMessage msg = new NetworkMessage("REGISTER", "onlyusername");
    msg.setRequestId(reqId);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm chứng: Server bắt được Exception và trả tin lỗi về máy khách
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Vui lòng nhập đầy đủ thông tin đăng ký!"), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM NẠP TIỀN VÀO TÀI KHOẢN (ADD_BALANCE)
  // ==============================================================
  @Test
  void testProcessRequest_AddBalance_Success() {
    String reqId = "balance-123";
    NetworkMessage msg = new NetworkMessage("ADD_BALANCE", "500000"); // Nạp 500k
    msg.setRequestId(reqId);

    // Giả lập: User đã đăng nhập thành công và là Customer
    Customer mockCustomer = new Customer("buyer", "pass", BigDecimal.ZERO);
    mockCustomer.setId(10);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm chứng: Đã cộng tiền trong DB và cập nhật Session
    verify(userSqlDAO, times(1)).updateBalance(eq(10), eq(new BigDecimal("500000")));
    verify(clientHandler, times(1)).sendResponse(eq("UPDATE_BALANCE_SUCCESS"), eq(mockCustomer), eq(reqId));
  }

  @Test
  void testProcessRequest_AddBalance_Failure_NotLoggedIn() {
    String reqId = "balance-fail";
    NetworkMessage msg = new NetworkMessage("ADD_BALANCE", "100000");
    msg.setRequestId(reqId);

    // Giả lập: Chưa đăng nhập (trả về null)
    when(clientHandler.getLoggedInUser()).thenReturn(null);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Vui lòng đăng nhập!"), eq(reqId));
  }

  @Test
  void testProcessRequest_AddBalance_Failure_InvalidAmount() {
    String reqId = "balance-zero";
    NetworkMessage msg = new NetworkMessage("ADD_BALANCE", "-50000"); // Số tiền âm trái luật
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("buyer", "pass", BigDecimal.ZERO);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Số tiền phải lớn hơn 0!"), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM KHÓA/MỞ KHÓA TÀI KHOẢN (BAN / UNBAN USER)
  // ==============================================================
  @Test
  void testProcessRequest_BanUser_Success() {
    String reqId = "ban-abc";
    // Gửi ID người dùng cần khóa (ví dụ: 55)
    NetworkMessage msg = new NetworkMessage("BAN_USER", 55);
    msg.setRequestId(reqId);

    when(userSqlDAO.updateBanStatus(55, true)).thenReturn(true);

    requestProcessor.processRequest(msg, clientHandler);

    verify(userSqlDAO, times(1)).updateBanStatus(55, true);
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đã khóa tài khoản thành công."), eq(reqId));
  }

  @Test
  void testProcessRequest_BanUser_Failed_DbError() {
    String reqId = "ban-fail";
    NetworkMessage msg = new NetworkMessage("BAN_USER", 999);
    msg.setRequestId(reqId);

    // Giả lập: Không tìm thấy ID để cập nhật trạng thái trong DB (trả về false)
    when(userSqlDAO.updateBanStatus(999, true)).thenReturn(false);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Không thể cập nhật trạng thái khóa tài khoản."), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM TẠO PHIÊN ĐẤU GIÁ (CREATE_AUCTION)
  // ==============================================================
  @Test
  void testProcessRequest_CreateAuction_Failure_NotSeller() {
    String reqId = "auction-fail";
    // Cú pháp: "itemId startPrice durationMinutes"
    NetworkMessage msg = new NetworkMessage("CREATE_AUCTION", "1 50000 30");
    msg.setRequestId(reqId);

    // Giả lập: User đăng nhập không phải là Customer (ví dụ đang null)
    when(clientHandler.getLoggedInUser()).thenReturn(null);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Phải đăng nhập bằng tài khoản người bán!"), eq(reqId));
  }

  @Test
  void testProcessRequest_CreateAuction_Failure_InvalidSyntax() {
    String reqId = "auction-syntax";
    // Thiếu tham số durationMinutes
    NetworkMessage msg = new NetworkMessage("CREATE_AUCTION", "1 50000");
    msg.setRequestId(reqId);

    Customer mockSeller = new Customer("seller", "pass", BigDecimal.ZERO);
    when(clientHandler.getLoggedInUser()).thenReturn(mockSeller);

    requestProcessor.processRequest(msg, clientHandler);

    String expectedErrorMsg = "Sai cú pháp! Gửi: itemId startPrice durationMinutes bidIncrement";
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq(expectedErrorMsg), eq(reqId));  }

  // ==============================================================
  // THỬ NGHIỆM ĐĂNG XUẤT HỆ THỐNG (LOGOUT)
  // ==============================================================
  @Test
  void testProcessRequest_Logout_Success() {
    String reqId = "logout-id";
    NetworkMessage msg = new NetworkMessage("LOGOUT", "");
    msg.setRequestId(reqId);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm chứng: Xóa session đăng nhập (gán null) và báo thành công
    verify(clientHandler, times(1)).setLoggedInUser(null);
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đã đăng xuất khỏi hệ thống."), eq(reqId));
  }

  // ==============================================================
  // 1. KIỂM THỬ XEM LỊCH SỬ ĐẤU GIÁ (GET_BIDDER_HISTORY)
  // ==============================================================
  @Test
  void testProcessRequest_GetBidderHistory_Success() {
    String reqId = "hist-01";
    // Giả lập data gửi lên là ID của người dùng (Ví dụ: 10)
    NetworkMessage msg = new NetworkMessage("GET_BIDDER_HISTORY", 10);
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("bidder1", "password", BigDecimal.ZERO);
    mockCustomer.setId(10);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    // Giả lập danh sách phiên đấu giá đã kết thúc từ DB
    List<Auction> fakeAuctions = new ArrayList<>();
    Auction finishedAuction = new Auction();
    finishedAuction.setId(101);
    finishedAuction.setStatus("FINISHED");
    fakeAuctions.add(finishedAuction);

    when(auctionSqlDAO.getFastFinishedAuctionsForBidder(10)).thenReturn(fakeAuctions);

    requestProcessor.processRequest(msg, clientHandler);

    // Xác minh Server đã truy vấn DB và trả kết quả thành công cho người dùng
    verify(auctionSqlDAO, times(1)).getFastFinishedAuctionsForBidder(10);
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), any(), eq(reqId));
  }

  @Test
  void testProcessRequest_GetBidderHistory_Forbidden() {
    String reqId = "hist-fail";
    // Người dùng có ID 10 nhưng lại cố tình đòi xem lịch sử của ID 99
    NetworkMessage msg = new NetworkMessage("GET_BIDDER_HISTORY", 99);
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("bidder1", "password", BigDecimal.ZERO);
    mockCustomer.setId(10);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    // Phải chặn lại và trả về lỗi bảo mật
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Không được xem lịch sử của người khác!"), eq(reqId));
  }

  // ==============================================================
  // 2. KIỂM THỬ ĐẤU GIÁ TỰ ĐỘNG (SET_AUTO_BID / REMOVE_AUTO_BID)
  // ==============================================================
  @Test
  void testProcessRequest_SetAutoBid_Success() {
    String reqId = "autobid-01";
    // Định dạng gửi lên: "auctionId maxBid" -> Ví dụ phiên 200, giá tối đa 1500000
    NetworkMessage msg = new NetworkMessage("SET_AUTO_BID", "200 1500000");
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("bidder1", "password", BigDecimal.ZERO);
    mockCustomer.setId(5);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    when(auctionSqlDAO.isBidderRegistered(200, 5)).thenReturn(true);

    requestProcessor.processRequest(msg, clientHandler);

    verify(auctionSqlDAO, times(1)).setAutoBid(200, 5, new BigDecimal("1500000"));
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đã bật đấu giá tự động!"), eq(reqId));
  }

  @Test
  void testProcessRequest_RemoveAutoBid_Success() {
    String reqId = "autobid-remove";
    NetworkMessage msg = new NetworkMessage("REMOVE_AUTO_BID", 200);
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("bidder1", "password", BigDecimal.ZERO);
    mockCustomer.setId(5);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    verify(auctionSqlDAO, times(1)).removeAutoBid(200, 5);
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đã tắt đấu giá tự động!"), eq(reqId));
  }

  // ─── 4. KIỂM THỬ ĐẶT GIÁ THẦU (BID) & CÀI ĐẶT ĐẤU GIÁ TỰ ĐỘNG ──────────────
  @Test
  void testProcessRequest_Bid_ValidationErrors() {
    String reqId = "bid-errors";

    // Lỗi 1: Chỉ Khách hàng mới được đặt giá (Admin gọi lệnh sẽ lỗi)
    com.uet.bidding.model.Admin admin = new com.uet.bidding.model.Admin();
    when(clientHandler.getLoggedInUser()).thenReturn(admin);
    requestProcessor.processRequest(new NetworkMessage("BID", "100 50000"), clientHandler);

    // Lỗi 2: Sai cú pháp chuỗi gói tin (Thiếu tham số giá tiền)
    Customer customer = new Customer("buyer", "pass", BigDecimal.ZERO);
    when(clientHandler.getLoggedInUser()).thenReturn(customer);
    requestProcessor.processRequest(new NetworkMessage("BID", "100"), clientHandler); // Không có dấu cách chia tiền

    verify(clientHandler, times(2)).sendResponse(eq("ERROR"), anyString(), any());
  }

  // ==============================================================
  // 1. TEST THÊM SẢN PHẨM ĐIỆN TỬ THÀNH CÔNG (ELECTRONICS_SUCCESS)
  // ==============================================================
  @Test
  void testProcessRequest_AddItem_Electronics_Success() {
    String reqId = "item-elec-success";

    // Tạo đối tượng JsonObject thật để mô phỏng đúng gói tin từ Client gửi lên
    com.google.gson.JsonObject itemJsonObj = new com.google.gson.JsonObject();
    itemJsonObj.addProperty("itemType", "ELECTRONICS");
    itemJsonObj.addProperty("name", "iPhone 15 Pro");
    itemJsonObj.addProperty("description", "Máy quốc tế");
    itemJsonObj.addProperty("startingPrice", "20000000");
    itemJsonObj.addProperty("brand", "Apple");
    itemJsonObj.addProperty("warrantyMonths", 12);
    itemJsonObj.addProperty("city", "Hà Nội");
    // Giả lập có trường imageBase64 null hoặc chuỗi rỗng để tránh log lỗi ảnh
    itemJsonObj.addProperty("imageBase64", "");

    // Truyền thẳng JsonObject này vào NetworkMessage giống hệt Client thật làm
    NetworkMessage msg = new NetworkMessage("ADD_ITEM", itemJsonObj);
    msg.setRequestId(reqId);

    // Giả lập trạng thái đăng nhập
    Customer mockCustomer = new Customer("seller1", "password", BigDecimal.ZERO);
    mockCustomer.setId(8);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    // Thực thi xử lý
    requestProcessor.processRequest(msg, clientHandler);

    // KHẮC PHỤC LỖI: Bây giờ chắc chắn hàm addItem trong DB sẽ được invoke!
    verify(itemSqlDAO, times(1)).addItem(any());
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đã gửi yêu cầu phê duyệt!"), eq(reqId));
  }

  @Test
  void testProcessRequest_AddItem_Success_Art_And_Vehicle() {
    String reqId = "add-item-art-veh";
    Customer customer = new Customer("seller1", "pass", BigDecimal.ZERO);
    customer.setId(10);
    when(clientHandler.getLoggedInUser()).thenReturn(customer);

    // Kịch bản A: Sản phẩm Nghệ thuật (ART)
    com.google.gson.JsonObject artObj = new com.google.gson.JsonObject();
    artObj.addProperty("itemType", "ART");
    artObj.addProperty("name", "Mona Lisa");
    artObj.addProperty("startingPrice", "1000000"); // Dạng chuỗi số
    artObj.addProperty("author", "Da Vinci");
    artObj.addProperty("creationYear", 1503);

    requestProcessor.processRequest(new NetworkMessage("ADD_ITEM", artObj), clientHandler);

    // Kịch bản B: Sản phẩm Phương tiện (VEHICLE)
    com.google.gson.JsonObject vehObj = new com.google.gson.JsonObject();
    vehObj.addProperty("itemType", "VEHICLE");
    vehObj.addProperty("name", "Tesla Model 3");
    vehObj.addProperty("startingPrice", new BigDecimal("45000")); // Dạng BigDecimal
    vehObj.addProperty("brand", "Tesla");
    vehObj.addProperty("model", "Long Range");
    vehObj.addProperty("manufacturingYear", 2023);
    vehObj.addProperty("mileage", 5000.5);
    vehObj.addProperty("engineType", "Điện");
    vehObj.addProperty("fuelType", "Pin EV");

    requestProcessor.processRequest(new NetworkMessage("ADD_ITEM", vehObj), clientHandler);

    verify(itemSqlDAO, times(2)).addItem(any(com.uet.bidding.model.Item.class));
  }

  @Test
  void testProcessRequest_AddItem_Failures() {
    String reqId = "add-item-fail";

    // Luồng 1: Chưa đăng nhập
    when(clientHandler.getLoggedInUser()).thenReturn(null);
    requestProcessor.processRequest(new NetworkMessage("ADD_ITEM", ""), clientHandler);

    // Luồng 2: Loại sản phẩm không hỗ trợ (Nhánh default của switch-case)
    Customer customer = new Customer("seller1", "pass", BigDecimal.ZERO);
    when(clientHandler.getLoggedInUser()).thenReturn(customer);

    com.google.gson.JsonObject badObj = new com.google.gson.JsonObject();
    badObj.addProperty("itemType", "FOOD"); // Không hỗ trợ
    badObj.addProperty("name", "Bánh mì");
    badObj.addProperty("startingPrice", 2);

    requestProcessor.processRequest(new NetworkMessage("ADD_ITEM", badObj), clientHandler);

    // Luồng 3: Thiếu hoặc lỗi trường giá khởi điểm (readBigDecimal Exception)
    com.google.gson.JsonObject emptyPriceObj = new com.google.gson.JsonObject();
    emptyPriceObj.addProperty("itemType", "ELECTRONICS");
    emptyPriceObj.addProperty("name", "Lỗi giá");
    emptyPriceObj.addProperty("startingPrice", "   "); // Trống rỗng

    requestProcessor.processRequest(new NetworkMessage("ADD_ITEM", emptyPriceObj), clientHandler);

    verify(clientHandler, times(3)).sendResponse(eq("ERROR"), anyString(), any());
  }

  // ==============================================================
  // 2. TEST THÊM SẢN PHẨM LỖI ĐỊNH DẠNG GIÁ (INVALID_PRICE)
  // ==============================================================
  @Test
  void testProcessRequest_AddItem_InvalidPrice() {
    String reqId = "item-fail-price";

    com.google.gson.JsonObject itemJsonObj = new com.google.gson.JsonObject();
    itemJsonObj.addProperty("itemType", "ART");
    itemJsonObj.addProperty("name", "Tranh Đông Hồ");
    itemJsonObj.addProperty("startingPrice", "Gia_Vo_Ly");
    itemJsonObj.addProperty("imageBase64", "");

    NetworkMessage msg = new NetworkMessage("ADD_ITEM", itemJsonObj);
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("seller1", "password", BigDecimal.ZERO);
    mockCustomer.setId(8);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    verify(itemSqlDAO, never()).addItem(any());

    // 🎯 ĐÃ CẬP NHẬT: Loại bỏ chuỗi "Lỗi định dạng giá: " để khớp hoàn toàn với thực tế hệ thống bắn ra
    String exactErrorMsg = "Character G is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.";

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq(exactErrorMsg), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM ĐỔI MẬT KHẨU (CHANGE_PASSWORD)
  // ==============================================================
  @Test
  void testProcessRequest_ChangePassword_Failure_NotLoggedIn() {
    String reqId = "cpwd-no-login";
    NetworkMessage msg = new NetworkMessage("CHANGE_PASSWORD", "oldPass|newPass|newPass");
    msg.setRequestId(reqId);

    // Chưa đăng nhập
    when(clientHandler.getLoggedInUser()).thenReturn(null);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Bạn chưa đăng nhập!"), eq(reqId));
  }

  @Test
  void testProcessRequest_ChangePassword_Success() throws Exception {
    String reqId = "cpwd-success";
    // Data: oldPass | newPass | confirmPass
    NetworkMessage msg = new NetworkMessage("CHANGE_PASSWORD", "123456|newpass123|newpass123");
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("testuser", "hashed_pass_placeholder", BigDecimal.ZERO);
    mockCustomer.setId(99);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    // Cần tạo một mật khẩu đã mã hóa thật bằng BCrypt để vượt qua vòng check BCrypt.checkpw
    String realHashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw("123456", org.mindrot.jbcrypt.BCrypt.gensalt());
    Customer dbCustomer = new Customer("testuser", realHashedPassword, BigDecimal.ZERO);
    dbCustomer.setId(99);

    when(userSqlDAO.findById(99)).thenReturn(dbCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    // Xác minh đã gọi DAO để update password và phản hồi SUCCESS
    verify(userSqlDAO, times(1)).updatePassword(99, "123456", "newpass123");
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đổi mật khẩu thành công!"), eq(reqId));
  }

  @Test
  void testProcessRequest_ChangePassword_WrongOldPassword() {
    String reqId = "pass-wrong-old";
    NetworkMessage msg = new NetworkMessage("CHANGE_PASSWORD", "wrong_old_pass|newPass123|newPass123");
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("testuser", "correct_old_hashed", BigDecimal.ZERO);
    mockCustomer.setId(1);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);
    when(userSqlDAO.findById(1)).thenReturn(mockCustomer);

    // Sử dụng BCrypt tạo một chuỗi hash hợp lệ khác để giả lập việc kiểm tra pass cũ sai lệch
    String anotherHash = org.mindrot.jbcrypt.BCrypt.hashpw("different_pass", org.mindrot.jbcrypt.BCrypt.gensalt(4));
    Customer dbUser = new Customer("testuser", anotherHash, BigDecimal.ZERO);
    when(userSqlDAO.findById(1)).thenReturn(dbUser);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Mật khẩu cũ không chính xác!"), eq(reqId));
  }

  @Test
  void testProcessRequest_ChangePassword_SameAsOld() {
    String reqId = "pass-same-old";
    String rawOld = "old123";
    String oldHashed = org.mindrot.jbcrypt.BCrypt.hashpw(rawOld, org.mindrot.jbcrypt.BCrypt.gensalt(4));

    NetworkMessage msg = new NetworkMessage("CHANGE_PASSWORD", rawOld + "|" + rawOld + "|" + rawOld);
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("testuser", oldHashed, BigDecimal.ZERO);
    mockCustomer.setId(1);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);
    when(userSqlDAO.findById(1)).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Mật khẩu mới phải khác với mật khẩu hiện tại!"), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM TÍNH NĂNG ADMIN (DUYỆT/TỪ CHỐI SẢN PHẨM & LẤY LIST)
  // ==============================================================
  @Test
  void testProcessRequest_ApproveItem_NotFound() {
    String reqId = "admin-approve-notfound";
    NetworkMessage msg = new NetworkMessage("APPROVE_ITEM", 999);
    msg.setRequestId(reqId);

    com.uet.bidding.model.Admin admin = new com.uet.bidding.model.Admin();
    when(clientHandler.getLoggedInUser()).thenReturn(admin);
    when(itemSqlDAO.findById(999)).thenReturn(null);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Sản phẩm không tồn tại."), eq(reqId));
  }

  @Test
  void testProcessRequest_AdminActions_ForbiddenForCustomer() {
    String reqId = "admin-forbidden";
    NetworkMessage msgGet = new NetworkMessage("GET_PENDING_ITEMS", "");
    msgGet.setRequestId(reqId);

    // Giả lập là Customer chứ không phải Admin
    Customer customer = new Customer("user", "pass", BigDecimal.ZERO);
    when(clientHandler.getLoggedInUser()).thenReturn(customer);

    requestProcessor.processRequest(msgGet, clientHandler);
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Bạn không có quyền thực hiện chức năng này!"), eq(reqId));

    // Kiểm tra tương tự với APPROVE_ITEM
    NetworkMessage msgApprove = new NetworkMessage("APPROVE_ITEM", 123);
    msgApprove.setRequestId(reqId);
    requestProcessor.processRequest(msgApprove, clientHandler);
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Bạn không có quyền thực hiện!"), eq(reqId));
  }

  @Test
  void testProcessRequest_GetPendingItems_Success() throws Exception {
    String reqId = "pending-123";
    NetworkMessage msg = new NetworkMessage("GET_PENDING_ITEMS", "");
    msg.setRequestId(reqId);

    // 🎯 ĐÃ SỬA: Giả lập kỹ hơn để Server nhận diện đúng là Admin
    com.uet.bidding.model.Admin mockAdmin = mock(com.uet.bidding.model.Admin.class);
    // Nếu hàm check quyền của bạn dùng instanceof, mock() đã lo.
    // Nếu hàm dùng getRole(), ta cần giả lập trả về "ADMIN" để vượt qua lớp bảo mật
    lenient().when(mockAdmin.getRole()).thenReturn("ADMIN");
    when(clientHandler.getLoggedInUser()).thenReturn(mockAdmin);

    List<com.uet.bidding.model.Item> mockList = new ArrayList<>();
    when(itemSqlDAO.getItemsByStatus("PENDING")).thenReturn(mockList);

    requestProcessor.processRequest(msg, clientHandler);

    verify(itemSqlDAO, times(1)).getItemsByStatus("PENDING");
    verify(clientHandler, times(1)).sendResponse(eq("GET_PENDING_ITEMS_SUCCESS"), eq(mockList), eq(reqId));
  }

  @Test
  void testProcessRequest_GetPendingItems_Failure_NotAdmin() {
    String reqId = "pending-fail";
    NetworkMessage msg = new NetworkMessage("GET_PENDING_ITEMS", "");
    msg.setRequestId(reqId);

    // Là Customer, không phải Admin
    Customer mockCustomer = new Customer("buyer", "pass", BigDecimal.ZERO);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Bạn không có quyền thực hiện chức năng này!"), eq(reqId));
  }

  @Test
  void testProcessRequest_ApproveItem_Failure_Exception() {
    String reqId = "approve-exception";
    NetworkMessage msg = new NetworkMessage("APPROVE_ITEM", 15);
    msg.setRequestId(reqId);

    com.uet.bidding.model.Admin admin = new com.uet.bidding.model.Admin();
    when(clientHandler.getLoggedInUser()).thenReturn(admin);

    // Ép DAO ném ra Exception để đi vào khối catch(Exception e)
    when(itemSqlDAO.findById(15)).thenThrow(new RuntimeException("Lỗi kết nối DB khi tìm kiếm"));

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Lỗi xử lý: Lỗi kết nối DB khi tìm kiếm"), eq(reqId));
  }

  @Test
  void testProcessRequest_ApproveItem_Success() throws Exception {
    String reqId = "approve-123";
    NetworkMessage msg = new NetworkMessage("APPROVE_ITEM", 15);
    msg.setRequestId(reqId);

    // 1. Giả lập quyền ADMIN
    com.uet.bidding.model.Admin mockAdmin = mock(com.uet.bidding.model.Admin.class);
    lenient().when(mockAdmin.getRole()).thenReturn("ADMIN");
    when(clientHandler.getLoggedInUser()).thenReturn(mockAdmin);

    // 2. Giả lập Abstract Class Item (Chỉ cần mock, không cần gán getter vì code thật không dùng)
    com.uet.bidding.model.Item mockItem = mock(com.uet.bidding.model.Item.class);

    // 3. Cấu hình hành vi cho DAO (Chỉ giữ lại 2 hàm mà code thật chắc chắn gọi)
    when(itemSqlDAO.findById(15)).thenReturn(mockItem);
    when(itemSqlDAO.updateItemStatus(15, "APPROVED")).thenReturn(true);

    // 4. Thực thi xử lý
    requestProcessor.processRequest(msg, clientHandler);

    // 5. Kiểm chứng kết quả
    verify(itemSqlDAO, times(1)).updateItemStatus(15, "APPROVED");
    verify(clientHandler, times(1)).sendResponse(eq("APPROVE_SUCCESS"), eq("Đã duyệt sản phẩm thành công!"), eq(reqId));
  }

  @Test
  void testProcessRequest_RejectItem_Success() throws Exception {
    String reqId = "reject-123";
    NetworkMessage msg = new NetworkMessage("REJECT_ITEM", "15|Ảnh không hợp lệ");
    msg.setRequestId(reqId);

    com.uet.bidding.model.Admin mockAdmin = mock(com.uet.bidding.model.Admin.class);
    // 🎯 ĐÃ SỬA: Giả lập quyền "ADMIN"
    lenient().when(mockAdmin.getRole()).thenReturn("ADMIN");
    when(clientHandler.getLoggedInUser()).thenReturn(mockAdmin);

    when(itemSqlDAO.updateItemStatus(15, "REJECTED", "Ảnh không hợp lệ")).thenReturn(true);

    requestProcessor.processRequest(msg, clientHandler);

    verify(itemSqlDAO, times(1)).updateItemStatus(15, "REJECTED", "Ảnh không hợp lệ");
    verify(clientHandler, times(1)).sendResponse(eq("REJECT_SUCCESS"), eq("Đã từ chối phê duyệt sản phẩm."), eq(reqId));
  }

  @Test
  void testProcessRequest_RejectItem_Failure_DbUpdateFalse() {
    String reqId = "reject-db-false";
    NetworkMessage msg = new NetworkMessage("REJECT_ITEM", "15|Ảnh không hợp lệ");
    msg.setRequestId(reqId);

    com.uet.bidding.model.Admin admin = new com.uet.bidding.model.Admin();
    when(clientHandler.getLoggedInUser()).thenReturn(admin);

    // Giả lập trường hợp DAO trả về false (Không tìm thấy hoặc không update được)
    when(itemSqlDAO.updateItemStatus(15, "REJECTED", "Ảnh không hợp lệ")).thenReturn(false);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Không thể cập nhật trạng thái từ chối."), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM THỐNG KÊ (GET_SYSTEM_STATS)
  // ==============================================================
  @Test
  void testProcessRequest_GetSystemStats_Success() throws Exception {
    String reqId = "stats-123";
    NetworkMessage msg = new NetworkMessage("GET_SYSTEM_STATS", "");
    msg.setRequestId(reqId);

    // Mock kết quả từ 3 DAO
    when(userSqlDAO.getTotalUserCount()).thenReturn(50);
    when(auctionSqlDAO.getActiveAuctionsCount()).thenReturn(10);
    when(itemSqlDAO.getPendingItemsCount()).thenReturn(5);

    requestProcessor.processRequest(msg, clientHandler);

    // Dùng ArgumentCaptor hoặc xác nhận chuỗi JSON được trả về
    verify(userSqlDAO, times(1)).getTotalUserCount();
    verify(auctionSqlDAO, times(1)).getActiveAuctionsCount();
    verify(itemSqlDAO, times(1)).getPendingItemsCount();

    // Vì JsonObject sẽ được match với any() do khó so sánh equals trực tiếp
    verify(clientHandler, times(1)).sendResponse(eq("GET_SYSTEM_STATS_SUCCESS"), any(com.google.gson.JsonObject.class), eq(reqId));
  }

  @Test
  void testProcessRequest_GetSystemStats_Exception() {
    String reqId = "stats-fail";
    NetworkMessage msg = new NetworkMessage("GET_SYSTEM_STATS", "");
    msg.setRequestId(reqId);

    when(userSqlDAO.getTotalUserCount()).thenThrow(new RuntimeException("Lỗi kết nối DB"));

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Lỗi tổng hợp số liệu thống kê: Lỗi kết nối DB"), eq(reqId));
  }

  // ==============================================================
  // THỬ NGHIỆM XÓA SẢN PHẨM & MỞ KHÓA USER
  // ==============================================================
  @Test
  void testProcessRequest_DeleteItem_Success() throws Exception {
    String reqId = "delete-item-123";
    NetworkMessage msg = new NetworkMessage("DELETE_ITEM", 404);
    msg.setRequestId(reqId);

    when(itemSqlDAO.deleteItemCompletely(404)).thenReturn(true);

    requestProcessor.processRequest(msg, clientHandler);

    verify(itemSqlDAO, times(1)).deleteItemCompletely(404);
    verify(clientHandler, times(1)).sendResponse(eq("DELETE_ITEM_SUCCESS"), eq("Xóa sản phẩm thành công!"), eq(reqId));
  }

  @Test
  void testProcessRequest_DeleteItem_Failure_Exception() {
    String reqId = "delete-item-exception";
    NetworkMessage msg = new NetworkMessage("DELETE_ITEM", 404);
    msg.setRequestId(reqId);

    when(itemSqlDAO.deleteItemCompletely(404)).thenThrow(new RuntimeException("Lỗi IO xóa file ảnh Cloud"));

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("DELETE_ITEM_FAILED"), eq("Lỗi Server: Lỗi IO xóa file ảnh Cloud"), eq(reqId));
  }

  @Test
  void testProcessRequest_UnbanUser_Success() {
    String reqId = "unban-abc";
    NetworkMessage msg = new NetworkMessage("UNBAN_USER", 55);
    msg.setRequestId(reqId);

    when(userSqlDAO.updateBanStatus(55, false)).thenReturn(true);

    requestProcessor.processRequest(msg, clientHandler);

    verify(userSqlDAO, times(1)).updateBanStatus(55, false);
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Mở khóa tài khoản thành công."), eq(reqId));
  }

  // ─── 3. KIỂM THỬ TOÀN DIỆN LỆNH LẤY TẤT CẢ USER (GET_ALL_USERS) ───────────
  @Test
  void testProcessRequest_GetAllUsers_Exception() {
    String reqId = "get-users-exception";
    NetworkMessage msg = new NetworkMessage("GET_ALL_USERS", "");
    msg.setRequestId(reqId);

    // Ép DAO ném ra lỗi để bao phủ block catch của GET_ALL_USERS
    when(userSqlDAO.getAllUsers()).thenThrow(new RuntimeException("TiDB Cloud timeout"));

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Lỗi lấy danh sách user: TiDB Cloud timeout"), eq(reqId));
  }

  // ─── 6. BẮT NGOẠI LỆ TỔNG CHO TOÀN BỘ HÀM PROCESS_REQUEST ──────────────────

  @Test
  void testProcessRequest_Global_Exception() {
    String reqId = "global-exception";
    // Truyền một NetworkMessage có loại lệnh null để ép switch-case bắn thẳng ra NullPointerException
    NetworkMessage msg = new NetworkMessage(null, "");
    msg.setRequestId(reqId);

    requestProcessor.processRequest(msg, clientHandler);

    // Kiểm tra xem khối bọc try-catch lớn ngoài cùng có bắt được và trả response ERROR không
    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), startsWith("Lỗi hệ thống:"), eq(reqId));
  }


  // ─── 3. KIỂM THỬ THÊM MỚI ĐÁNH GIÁ (ADD_REVIEW) ───────────────────────────
  @Test
  void testProcessRequest_AddReview_ValidationAndErrors() {
    String reqId = "add-review-val";
    Customer customer = new Customer("bidder1", "pass", BigDecimal.ZERO);
    customer.setId(22);
    when(clientHandler.getLoggedInUser()).thenReturn(customer);

    // Trường hợp 1: Số sao không hợp lệ (Ví dụ: 6 sao)
    com.google.gson.JsonObject revBadStars = new com.google.gson.JsonObject();
    revBadStars.addProperty("auctionId", 10);
    revBadStars.addProperty("sellerId", 5);
    revBadStars.addProperty("stars", 6); // LỖI
    revBadStars.addProperty("comment", "Fake review");

    requestProcessor.processRequest(new NetworkMessage("ADD_REVIEW", revBadStars), clientHandler);

    // Trường hợp 2: Phiên đấu giá không tồn tại trong hệ thống
    com.google.gson.JsonObject revNoAuction = new com.google.gson.JsonObject();
    revNoAuction.addProperty("auctionId", 999);
    revNoAuction.addProperty("sellerId", 5);
    revNoAuction.addProperty("stars", 5);
    revNoAuction.addProperty("comment", "Good");
    when(auctionSqlDAO.findById(999)).thenReturn(null); // Trả về null

    requestProcessor.processRequest(new NetworkMessage("ADD_REVIEW", revNoAuction), clientHandler);

    // Trường hợp 3: Phiên chưa kết thúc (Trạng thái vẫn là RUNNING)
    com.google.gson.JsonObject revRunning = new com.google.gson.JsonObject();
    revRunning.addProperty("auctionId", 200);
    revRunning.addProperty("sellerId", 5);
    revRunning.addProperty("stars", 5);
    revRunning.addProperty("comment", "Good");

    Auction runningAuction = mock(Auction.class);
    when(runningAuction.getStatus()).thenReturn("RUNNING"); // Chưa FINISHED
    when(auctionSqlDAO.findById(200)).thenReturn(runningAuction);

    requestProcessor.processRequest(new NetworkMessage("ADD_REVIEW", revRunning), clientHandler);

    verify(clientHandler, times(3)).sendResponse(eq("ERROR"), anyString(), any());
  }

  // ─── 5. KIỂM THỬ CÁC LỆNH KẾT THÚC / QUẢN LÝ PHIÊN CỦA SELLER ──────────────
  @Test
  void testProcessRequest_SellerEndAuction_Errors() {
    Customer seller = new Customer("seller", "pass", BigDecimal.ZERO);
    seller.setId(7);
    when(clientHandler.getLoggedInUser()).thenReturn(seller);

    // Kịch bản lỗi: Thử dừng một phiên đấu giá thuộc về người khác
    Auction otherAuction = mock(Auction.class);
    com.uet.bidding.model.Item normalItem = mock(com.uet.bidding.model.Item.class);
    when(normalItem.getSellerId()).thenReturn(999); // Không trùng ID = 7 của người gọi
    when(otherAuction.getItem()).thenReturn(normalItem);
    when(auctionSqlDAO.findById(123)).thenReturn(otherAuction);

    requestProcessor.processRequest(new NetworkMessage("SELLER_END_AUCTION", 123), clientHandler);

    // Kịch bản lỗi: Phiên đấu giá vốn dĩ đã kết thúc từ trước
    Auction finishedAuction = mock(Auction.class);
    com.uet.bidding.model.Item myItem = mock(com.uet.bidding.model.Item.class);
    when(myItem.getSellerId()).thenReturn(7); // Đúng hàng của mình
    when(finishedAuction.getItem()).thenReturn(myItem);
    when(finishedAuction.getStatus()).thenReturn("FINISHED"); // Đã kết thúc
    when(auctionSqlDAO.findById(456)).thenReturn(finishedAuction);

    requestProcessor.processRequest(new NetworkMessage("SELLER_END_AUCTION", 456), clientHandler);

    verify(clientHandler, times(2)).sendResponse(eq("ERROR"), anyString(), any());
  }

  // ─── 6. QUÉT SẠCH LỆNH XEM KHO KHÁC & HÀM BẢO MẬT HỆ THỐNG ────────────────
  @Test
  void testProcessRequest_GetItemsBySeller_Forbidden() {
    Customer customer = new Customer("hacker", "pass", BigDecimal.ZERO);
    customer.setId(66);
    when(clientHandler.getLoggedInUser()).thenReturn(customer);

    // Yêu cầu xem kho của Seller có ID = 11 (Trong khi mình là ID = 66) -> Bị cấm lập tức!
    NetworkMessage msg = new NetworkMessage("GET_ITEMS_BY_SELLER", 11);
    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Không được xem kho người khác!"), any());
  }

  @Test
  void testProcessRequest_GetSellerActiveAuctions_Forbidden() {
    Customer customer = new Customer("hacker", "pass", BigDecimal.ZERO);
    customer.setId(66);
    when(clientHandler.getLoggedInUser()).thenReturn(customer);

    // Thử dòm ngó danh sách đấu giá riêng tư của tài khoản số 11
    NetworkMessage msg = new NetworkMessage("GET_SELLER_ACTIVE_AUCTIONS", 11);
    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Không được xem phiên đấu giá của người khác!"), any());
  }

  @Test
  void testProcessRequest_LoginLogic_RootBlock() {
    String reqId = "login-root";
    // Ép gửi tài khoản chứa từ khóa nhạy cảm "root" để kiểm thử cơ chế chặn hack cục bộ
    NetworkMessage msg = new NetworkMessage("LOGIN", "root_admin 123456");
    msg.setRequestId(reqId);

    requestProcessor.processRequest(msg, clientHandler);

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Tài khoản root không được phép truy cập từ Client!"), eq(reqId));
  }


}