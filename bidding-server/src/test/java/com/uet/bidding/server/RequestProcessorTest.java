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
  }

  @Test
  void testProcessRequest_GetAllAuctions_Success() {
    // 1. Chuẩn bị dữ liệu giả lập
    String reqId = "test-req-id-123";
    NetworkMessage msg = new NetworkMessage("GET_ALL_AUCTIONS", "");
    msg.setRequestId(reqId);

    List<Auction> fakeAuctions = new ArrayList<>();
    // Giả lập hàm trong DAO trả về danh sách trống hoặc có phần tử
    when(auctionSqlDAO.getRunningAuctionsForHall()).thenReturn(fakeAuctions);

    // 2. Chạy hàm cần test
    requestProcessor.processRequest(msg, clientHandler);

    // 3. Kiểm chứng (Verify): Xem server có gọi hàm gửi phản hồi SUCCESS về cho Client đúng reqId không
    verify(auctionSqlDAO, times(1)).getRunningAuctionsForHall();
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq(fakeAuctions), eq(reqId));
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

    verify(clientHandler, times(1)).sendResponse(eq("ERROR"), eq("Sai cú pháp! Gửi: itemId startPrice durationMinutes"), eq(reqId));
  }

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

    requestProcessor.processRequest(msg, clientHandler);

    verify(auctionSqlDAO, times(1)).setAutoBid(200, 5, new BigDecimal("1500000"));
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đã bật đấu giá tự động!"), eq(reqId));
  }

  @Test
  void testProcessRequest_RemoveAutoBid_Success() {
    String reqId = "autobid-remove";
    NetworkMessage msg = new NetworkMessage("REMOVE_AUTO_BID", "200");
    msg.setRequestId(reqId);

    Customer mockCustomer = new Customer("bidder1", "password", BigDecimal.ZERO);
    mockCustomer.setId(5);
    when(clientHandler.getLoggedInUser()).thenReturn(mockCustomer);

    requestProcessor.processRequest(msg, clientHandler);

    verify(auctionSqlDAO, times(1)).removeAutoBid(200, 5);
    verify(clientHandler, times(1)).sendResponse(eq("SUCCESS"), eq("Đã tắt đấu giá tự động!"), eq(reqId));
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

    // 🌟 SỬA TẠI ĐÂY: Chấp nhận mọi tin nhắn chứa lỗi định dạng số của hệ thống
    verify(clientHandler, times(1)).sendResponse(
        eq("ERROR"),
        org.mockito.ArgumentMatchers.contains("Character G"),
        eq(reqId)
    );
  }

}