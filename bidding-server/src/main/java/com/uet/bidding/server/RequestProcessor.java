package com.uet.bidding.server;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory; // Quan trọng nhất
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.dao.*;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.*;
import com.uet.bidding.service.AuctionManager;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;import java.util.List;

public class RequestProcessor {
  private final Gson gson = GsonFactory.getInstance();
  private final UserSqlDAO userSqlDAO;
  private final ItemSqlDAO itemSqlDAO;
  private final AuctionSqlDAO auctionSqlDAO;

  public RequestProcessor(UserSqlDAO userSqlDAO, ItemSqlDAO itemSqlDAO, AuctionSqlDAO auctionSqlDAO) {
    this.userSqlDAO = userSqlDAO;
    this.itemSqlDAO = itemSqlDAO;
    this.auctionSqlDAO = auctionSqlDAO;
  }

  public void processRequest(NetworkMessage msg, ClientHandler handler) {
    String reqId = msg.getRequestId();

    try {
      switch (msg.getType()) {
        case "LOGIN" -> handleLogin(msg, handler);
        case "REGISTER" -> handleRegisterRequest(msg, handler);
        case "UPDATE_PROFILE" -> handleUpdateProfile(msg, handler);
        case "ADD_BALANCE" -> handleAddBalance(msg, handler);
        case "BID" -> handleBid(msg, handler);
        case "GET_BID_HISTORY" -> handleGetBidHistory(msg, handler);
        case "GET_ALL_AUCTIONS" -> handler.sendResponse("SUCCESS", auctionSqlDAO.getAllAuctions(), reqId);
        case "CREATE_AUCTION" -> handleCreateAuction(msg, handler);
        case "SET_AUTO_BID" -> handleSetAutoBid(msg, handler);
        case "REMOVE_AUTO_BID" -> handleRemoveAutoBid(msg, handler);
        case "ADD_REVIEW" -> handleAddReview(msg, handler);
        case "GET_REVIEWS_BY_SELLER" -> handleGetReviewsBySeller(msg, handler);
        // ─── THÊM 3 CASE MỚI VÀO ĐÂY ĐỂ ĐIỀU HƯỚNG SỰ KIỆN ADMIN ───────────
        case "GET_PENDING_ITEMS" -> handleGetPendingItems(msg, handler);
        case "APPROVE_ITEM" -> handleApproveItem(msg, handler);
        case "REJECT_ITEM" -> handleRejectItem(msg, handler);
        // ─── THÊM CASE NÀY ĐỂ XỬ LÝ LỆNH THỐNG KÊ ─────────────────────────
        case "GET_SYSTEM_STATS" -> handleGetSystemStats(msg, handler);
        case "LOGOUT" -> {
          handler.setLoggedInUser(null);
          handler.sendResponse("SUCCESS", "Đã đăng xuất khỏi hệ thống.", reqId);
        }
        case "ADD_ITEM" -> handleAddItem(msg, handler);
        case "GET_ITEMS_BY_SELLER" -> handleGetItemsBySeller(msg, handler);
        case "GET_SELLER_ACTIVE_AUCTIONS" -> handleGetSellerAuctions(msg, handler, "RUNNING");
        case "GET_SELLER_FINISHED_AUCTIONS" -> handleGetSellerAuctions(msg, handler, "FINISHED");
        case "REGISTER_FOR_AUCTION" -> handleRegisterForAuction(msg, handler);
        case "GET_MY_REGISTRATIONS" -> handleGetMyRegistrations(msg, handler);
        case "IS_REGISTERED_FOR_AUCTION" -> handleIsRegisteredForAuction(msg, handler);
        // 1. Thêm vào switch-case trong processRequest
        case "GET_ITEM_IMAGE" -> handleGetItemImage(msg, handler);

        // Thêm 3 case này vào switch-case trong RequestProcessor.java của Server
        case "GET_ALL_USERS" -> {
          try {
            // Gọi DAO lấy dữ liệu từ database TiDB Cloud
            List<User> list = userSqlDAO.getAllUsers();

            // Phải gửi trả đúng chuỗi loại phản hồi mà Client đang đợi (ALL_USERS_RESPONSE)
            handler.sendResponse("ALL_USERS_RESPONSE", list, reqId);
          } catch (Exception e) {
            handler.sendResponse("ERROR", "Lỗi lấy danh sách user: " + e.getMessage(), reqId);
          }
        }

        case "BAN_USER" -> {
          int userId = ((Number) msg.getData()).intValue();
          // Gọi lệnh cập nhật trườngis_banned = TRUE trong Database
          boolean success = userSqlDAO.updateBanStatus(userId, true);
          if (success) {
            handler.sendResponse("SUCCESS", "Đã khóa tài khoản thành công.", reqId);
          } else {
            handler.sendResponse("ERROR", "Không thể cập nhật trạng thái khóa tài khoản.", reqId);
          }
        }

        case "UNBAN_USER" -> {
          int userId = ((Number) msg.getData()).intValue();
          // Gọi lệnh cập nhật trường is_banned = FALSE trong Database
          boolean success = userSqlDAO.updateBanStatus(userId, false);
          if (success) {
            handler.sendResponse("SUCCESS", "Mở khóa tài khoản thành công.", reqId);
          } else {
            handler.sendResponse("ERROR", "Không thể mở khóa tài khoản.", reqId);
          }
        }
        default -> handler.sendResponse("ERROR", "Lệnh không hợp lệ hoặc chưa được hỗ trợ!", reqId);
      }


    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi hệ thống: " + e.getMessage(), reqId);
      e.printStackTrace();
    }
  }

  // --- CÁC HÀM XỬ LÝ CHI TIẾT ĐƯỢC VIẾT THÊM VÀO PHÍA DƯỚI ---

  private void handleGetItemImage(NetworkMessage msg, ClientHandler handler) {
    try {
      int itemId = ((Number) msg.getData()).intValue();
      String path = itemSqlDAO.getImagePath(itemId);

      if (path == null || path.isEmpty()) {
        handler.sendResponse("ERROR", "Sản phẩm không có ảnh.", msg.getRequestId());
        return;
      }

      java.io.File file = new java.io.File(path);
      if (!file.exists()) {
        handler.sendResponse("ERROR", "File ảnh không tồn tại trên Server.", msg.getRequestId());
        return;
      }

      // Đọc file và chuyển sang Base64 để gửi qua mạng
      byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
      String base64 = java.util.Base64.getEncoder().encodeToString(fileContent);

      handler.sendResponse("GET_IMAGE_SUCCESS", base64, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi đọc ảnh: " + e.getMessage(), msg.getRequestId());
    }
  }

  /**
   * Xử lý gom số liệu đếm từ database TiDB Cloud gửi về cho màn hình Admin Thống kê
   */
  private void handleGetSystemStats(NetworkMessage msg, ClientHandler handler) {
    try {
      // 1. Thực hiện gọi SQL COUNT từ các DAO truy vấn dữ liệu (Đảm bảo DAO của bạn đã viết các hàm đếm này)
      int totalUsers = userSqlDAO.getTotalUserCount();
      int activeAuctions = auctionSqlDAO.getActiveAuctionsCount();
      int pendingItems = itemSqlDAO.getPendingItemsCount();

      // 2. Đóng gói 3 con số vào 1 JsonObject gọn gàng chuẩn cấu trúc Client đang bóc tách
      JsonObject statsJson = new JsonObject();
      statsJson.addProperty("totalUsers", totalUsers);
      statsJson.addProperty("activeAuctions", activeAuctions);
      statsJson.addProperty("pendingItems", pendingItems);

      // 3. Bắn trả kết quả về chính xác requestId của màn hình Admin đang đợi thông qua Type SUCCESS
      handler.sendResponse("GET_SYSTEM_STATS_SUCCESS", statsJson, msg.getRequestId());
      System.out.println("📊 [Server] Đã tổng hợp và gửi số liệu thống kê mới nhất cho Admin.");
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi tổng hợp số liệu thống kê: " + e.getMessage(), msg.getRequestId());
      e.printStackTrace();
    }
  }

  /**
   * 1. Xử lý lấy danh sách sản phẩm chưa duyệt gửi về cho Admin
   */
  private void handleGetPendingItems(NetworkMessage msg, ClientHandler handler) {
    if (!(handler.getLoggedInUser() instanceof Admin)) {
      handler.sendResponse("ERROR", "Bạn không có quyền thực hiện chức năng này!", msg.getRequestId());
      return;
    }
    try {
      // Khởi tạo DAO sản phẩm để truy vấn DB (giống cách làm ReviewSqlDAO của bạn)
      List<Item> pendingItems = itemSqlDAO.getItemsByStatus("PENDING");

      // Trả phản hồi kèm gói dữ liệu về chính xác RequestId đang đợi ở Client
      handler.sendResponse("GET_PENDING_ITEMS_SUCCESS", pendingItems, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi lấy danh sách chờ duyệt: " + e.getMessage(), msg.getRequestId());
    }
  }

  /**
   * 2. Xử lý cập nhật trạng thái APPROVED khi Admin duyệt bài
   */
  private void handleApproveItem(NetworkMessage msg, ClientHandler handler) {
    if (!(handler.getLoggedInUser() instanceof Admin)) {
      handler.sendResponse("ERROR", "Bạn không có quyền thực hiện!", msg.getRequestId());
      return;
    }

    try {
      int approveId = ((Number) msg.getData()).intValue();
      ItemSqlDAO itemDAO = new ItemSqlDAO();

      // 1. Lấy thông tin sản phẩm trước khi duyệt để lấy giá khởi điểm
      Item item = itemDAO.findById(approveId);
      if (item == null) {
        handler.sendResponse("ERROR", "Sản phẩm không tồn tại.", msg.getRequestId());
        return;
      }

      // 2. Cập nhật trạng thái thành APPROVED
      boolean success = itemDAO.updateItemStatus(approveId, "APPROVED");

      if (success) {
        // 3. TỰ ĐỘNG TẠO PHIÊN ĐẤU GIÁ (AUCTION)
        auctionSqlDAO.createAuction(
            item,
            item.getStartingPrice(), // Giá khởi điểm
            LocalDateTime.now(),     // Bắt đầu ngay
            LocalDateTime.now().plusDays(1), // Kết thúc sau 24h
            BigDecimal.valueOf(10000) // Bước giá mặc định
        );

        handler.sendResponse("APPROVE_SUCCESS", "Sản phẩm đã lên sàn!", msg.getRequestId());
        // Broadcast cho tất cả người dùng thấy sản phẩm mới
        Server.broadcast(new NetworkMessage("BROADCAST",
            "🔥 SÀN MỚI: '" + item.getName() + "' vừa lên kệ. Tham gia ngay!"));
      }
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi xử lý: " + e.getMessage(), msg.getRequestId());
    }
  }

  /**
   * 3. Xử lý cập nhật trạng thái REJECTED khi Admin từ chối bài
   */
  private void handleRejectItem(NetworkMessage msg, ClientHandler handler) {
    if (!(handler.getLoggedInUser() instanceof Admin)) {
      handler.sendResponse("ERROR", "Bạn không có quyền thực hiện chức năng này!", msg.getRequestId());
      return;
    }
    try {
      // Nhận chuỗi định dạng "ID|Lý do từ chối" truyền từ Client sang
      String rejectData = String.valueOf(msg.getData());
      String[] parts = rejectData.split("\\|");
      int rejectId = Integer.parseInt(parts[0]);
      String reason = parts.length > 1 ? parts[1] : "Không có lý do cụ thể.";

      boolean success = new ItemSqlDAO().updateItemStatus(rejectId, "REJECTED");
      if (success) {
        System.out.println("[Server] Đã từ chối SP #" + rejectId + ". Lý do: " + reason);
        handler.sendResponse("REJECT_SUCCESS", "Đã từ chối phê duyệt sản phẩm.", msg.getRequestId());
      } else {
        handler.sendResponse("ERROR", "Không thể cập nhật trạng thái từ chối.", msg.getRequestId());
      }
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi xử lý từ chối: " + e.getMessage(), msg.getRequestId());
    }
  }

  private void handleLogin(NetworkMessage msg, ClientHandler handler) {
    try {
      String credentials = String.valueOf(msg.getData());
      handleLoginLogic(credentials);
      String[] loginData = credentials.split(" ");
      if (loginData.length < 2) throw new AuthenticationException("Thiếu mật khẩu!");

      User user = userSqlDAO.checkLogin(loginData[0].trim(), loginData[1].trim());
      if (user != null) {
        if (user.isBanned()) throw new AuthenticationException("Tài khoản đã bị khóa!");
        handler.setLoggedInUser(user);
        handler.sendResponse("LOGIN_SUCCESS", user, msg.getRequestId());
      } else {
        throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
      }
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleRegisterRequest(NetworkMessage msg, ClientHandler handler) {
    try {
      handleRegister(String.valueOf(msg.getData()));
      handler.sendResponse("REGISTER_SUCCESS", "Đăng ký thành công!", msg.getRequestId());

      // 💡 THÊM DÒNG NÀY: Phát tín hiệu cho toàn mạng biết vừa có user mới
      Server.broadcast(new NetworkMessage("NEW_USER_REGISTERED", "Có tài khoản mới vừa gia nhập!"));

    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleUpdateProfile(NetworkMessage msg, ClientHandler handler) {
    try {
      String userJson = gson.toJson(msg.getData());
      JsonObject jsonObject = gson.fromJson(userJson, JsonObject.class);

      if (jsonObject.has("role") && "ADMIN".equals(jsonObject.get("role").getAsString())) {
        handler.sendResponse("ERROR", "Không thể cập nhật hồ sơ của Admin.", msg.getRequestId());
        return;
      }

      Customer customer = gson.fromJson(userJson, Customer.class);
      userSqlDAO.updateProfile(customer);
      handler.setLoggedInUser(customer);
      handler.sendResponse("UPDATE_PROFILE_SUCCESS", customer, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Cập nhật thất bại: " + e.getMessage(), msg.getRequestId());
    }
  }

  private void handleAddBalance(NetworkMessage msg, ClientHandler handler) throws Exception {
    if (handler.getLoggedInUser() == null) {
      handler.sendResponse("ERROR", "Vui lòng đăng nhập!", msg.getRequestId());
      return;
    }

    if (handler.getLoggedInUser() instanceof Customer loggedInCustomer) {
      BigDecimal amountToAdd = new BigDecimal(String.valueOf(msg.getData()));
      if (amountToAdd.compareTo(BigDecimal.ZERO) <= 0) {
        handler.sendResponse("ERROR", "Số tiền phải lớn hơn 0!", msg.getRequestId());
        return;
      }

      userSqlDAO.updateBalance(loggedInCustomer.getId(), amountToAdd);
      loggedInCustomer.addFunds(amountToAdd);
      handler.sendResponse("UPDATE_BALANCE_SUCCESS", loggedInCustomer, msg.getRequestId());
    } else {
      handler.sendResponse("ERROR", "Admin không có chức năng số dư!", msg.getRequestId());
    }
  }
  private void handleAddItem(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer c)) throw new UserException("Phải đăng nhập!");

      JsonObject itemJson = gson.toJsonTree(msg.getData()).getAsJsonObject();
      String imagePath = null;

      // 1. Lưu file xuống ổ cứng ngay và lấy path
      if (itemJson.has("imageBase64") && !itemJson.get("imageBase64").isJsonNull()) {
        imagePath = saveImageToFile(itemJson.get("imageBase64").getAsString());
      }

      // 2. Parse Item nhưng KHÔNG gán Base64 vào object Item
      Item item = parseItemFromJson(itemJson.toString());
      item.setSellerId(c.getId());
      item.setImagePath(imagePath); // Chỉ lưu đường dẫn này vào DB
      item.setImageData(null);      // Luôn để null để DB nhẹ tênh

      itemSqlDAO.addItem(item); // Giả sử bạn đã đổi DAO để nhận image_path
      handler.sendResponse("SUCCESS", "Đã gửi yêu cầu phê duyệt!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  // Hàm phụ lưu file ảnh vào thư mục storage của Server, ĐÃ SỬA ĐỂ XỬ LÝ XOAY ẢNH
  private String saveImageToFile(String base64Data) throws IOException {
    // 1. Giải mã Base64 sang Byte array
    byte[] imageBytes = Base64.getDecoder().decode(base64Data);

    // 2. Tạo file đích
    String fileName = "item_" + System.currentTimeMillis() + ".jpg";
    File dir = new File("server_storage/items");
    if (!dir.exists()) dir.mkdirs();
    File targetFile = new File(dir, fileName);

    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
      // 3. Đọc hướng ảnh (Orientation) từ Metadata
      int orientation = 1; // Mặc định là bình thường
      try {
        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
        // Sử dụng getFirstDirectoryOfType với Class chính xác
        ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
          orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        }
      } catch (Exception e) {
        System.out.println("Không tìm thấy metadata EXIF, giữ nguyên hướng gốc.");
      }

      // 4. Đọc ảnh vào BufferedImage để xử lý
      BufferedImage originalImage = ImageIO.read(bais);
      if (originalImage == null) throw new IOException("Định dạng ảnh không hỗ trợ.");

      // 5. Xoay ảnh dựa trên Orientation
      BufferedImage finalImage = originalImage;
      switch (orientation) {
        case 6 -> finalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_90);
        case 3 -> finalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_180);
        case 8 -> finalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_270);
      }

      // 6. Lưu ảnh đã xử lý xuống ổ cứng
      ImageIO.write(finalImage, "jpg", targetFile);
    }

    return targetFile.getPath();
  }

  private void handleBid(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer bidder)) {
        throw new InvalidBidException("Chỉ khách hàng mới được đặt giá!");
      }

      String[] bidParts = String.valueOf(msg.getData()).split(" ");
      if (bidParts.length < 2) throw new InvalidBidException("Sai cú pháp! Gửi: auctionId sốTiền");

      int auctionId = Integer.parseInt(bidParts[0]);
      BigDecimal bidAmount = new BigDecimal(bidParts[1]);

      LocalDateTime endBefore = auctionSqlDAO.findById(auctionId).getEndTime();

      boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, bidAmount);
      if (success) {
        Auction updated = auctionSqlDAO.findById(auctionId);

        boolean extended = updated.getEndTime().isAfter(endBefore);
        if (extended) {
          Server.broadcast(new NetworkMessage("BROADCAST",
              "⏱ Phiên #" + auctionId + " được gia hạn thêm "
                  + updated.getAntiSnipeExtensionMinutes() + " phút (anti-sniping)!"));
        }

        Server.broadcast(new NetworkMessage("BROADCAST",
            "Người dùng [" + bidder.getUsername() + "] đặt giá "
                + bidAmount + " cho phiên #" + auctionId));

        Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
        handler.sendResponse("SUCCESS", "Đặt giá thành công!", msg.getRequestId());
      }
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleCreateAuction(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer seller)) {
        throw new UserException("Phải đăng nhập bằng tài khoản người bán!");
      }
      String[] parts = String.valueOf(msg.getData()).trim().split("\\s+");
      if (parts.length < 3) {
        throw new UserException("Sai cú pháp! Gửi: itemId startPrice durationMinutes");
      }
      int itemId = Integer.parseInt(parts[0]);
      BigDecimal startPrice = new BigDecimal(parts[1]);
      int durationMinutes = Integer.parseInt(parts[2]);
      if (durationMinutes <= 0) {
        throw new UserException("Thời lượng phiên phải lớn hơn 0 phút!");
      }
      if (startPrice.compareTo(BigDecimal.ZERO) <= 0) {
        throw new UserException("Giá khởi điểm phải lớn hơn 0!");
      }

      Item item = new ItemSqlDAO().findById(itemId);
      if (item.getSellerId() != seller.getId()) {
        throw new UserException("Sản phẩm không thuộc kho hàng của bạn!");
      }
      if (item.isInAuction()) {
        throw new UserException("Sản phẩm đang trong một phiên đấu giá khác!");
      }

      item.setStartingPrice(startPrice);
      LocalDateTime endTime = LocalDateTime.now().plusMinutes(durationMinutes);
      Auction created = AuctionManager.getInstance().createAuction(item, endTime);
      created.setStatus("RUNNING");
      created.setRegisteredCount(0);

      Server.broadcast(new NetworkMessage("NEW_AUCTION_ADDED", created));
      Server.broadcast(new NetworkMessage("AUCTION_UPDATED", created));
      handler.sendResponse("SUCCESS", created, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleRegisterForAuction(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      if (!customer.hasCompleteProfile()) {
        throw new UserException("Hoàn thiện hồ sơ trước khi đăng ký tham gia!");
      }
      int auctionId = Integer.parseInt(String.valueOf(msg.getData()).trim());
      Auction auction = auctionSqlDAO.findById(auctionId);
      if (!"RUNNING".equals(auction.getStatus())) {
        throw new UserException("Phiên đấu giá không mở đăng ký!");
      }
      if (auction.getItem().getSellerId() == customer.getId()) {
        throw new UserException("Người bán không thể đăng ký phiên của chính mình!");
      }
      if (auctionSqlDAO.isBidderRegistered(auctionId, customer.getId())) {
        handler.sendResponse("SUCCESS", "ALREADY_REGISTERED", msg.getRequestId());
        return;
      }
      auctionSqlDAO.registerBidderForAuction(auctionId, customer.getId());
      Auction updated = auctionSqlDAO.findById(auctionId);
      updated.setRegisteredCount(auctionSqlDAO.getRegistrationCount(auctionId));
      Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
      handler.sendResponse("SUCCESS", updated, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetMyRegistrations(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      List<Integer> ids = auctionSqlDAO.getRegisteredAuctionIdsForBidder(customer.getId());
      handler.sendResponse("SUCCESS", ids, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleIsRegisteredForAuction(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      int auctionId = Integer.parseInt(String.valueOf(msg.getData()).trim());
      boolean registered = auctionSqlDAO.isBidderRegistered(auctionId, customer.getId());
      handler.sendResponse("SUCCESS", registered, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetBidHistory(NetworkMessage msg, ClientHandler handler) {
    try {
      int auctionId = Integer.parseInt(String.valueOf(msg.getData()));
      List<Bid> bids = new BidSqlDAO().getBidsByAuction(auctionId);
      handler.sendResponse("SUCCESS", bids, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleSetAutoBid(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập bằng tài khoản khách hàng!");
      }
      String[] parts = String.valueOf(msg.getData()).trim().split("\\s+");
      if (parts.length < 2) throw new UserException("Sai cú pháp! Gửi: auctionId maxBid");

      int auctionId = Integer.parseInt(parts[0]);
      BigDecimal maxBid = new BigDecimal(parts[1]);

      auctionSqlDAO.setAutoBid(auctionId, customer.getId(), maxBid);
      handler.sendResponse("SUCCESS", "Đã bật đấu giá tự động!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleRemoveAutoBid(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      int auctionId = Integer.parseInt(String.valueOf(msg.getData()).trim());
      auctionSqlDAO.removeAutoBid(auctionId, customer.getId());
      handler.sendResponse("SUCCESS", "Đã tắt đấu giá tự động!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleAddReview(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }

      JsonObject obj = gson.fromJson(gson.toJson(msg.getData()), JsonObject.class);
      int auctionId = obj.get("auctionId").getAsInt();
      int sellerId = obj.get("sellerId").getAsInt();
      int stars = obj.get("stars").getAsInt();
      String comment = obj.get("comment").getAsString();

      if (stars < 1 || stars > 5) throw new UserException("Số sao phải từ 1 đến 5!");

      Auction auction = auctionSqlDAO.findById(auctionId);
      if (!"FINISHED".equals(auction.getStatus())) {
        throw new UserException("Chỉ được đánh giá sau khi phiên đã kết thúc!");
      }
      if (auction.getHighestBidder() == null
          || auction.getHighestBidder().getId() != customer.getId()) {
        throw new UserException("Chỉ người thắng đấu giá mới được đánh giá!");
      }

      new ReviewSqlDAO().addReview(auctionId, sellerId, customer.getId(), stars, comment);
      handler.sendResponse("SUCCESS", "Cảm ơn bạn đã đánh giá!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetReviewsBySeller(NetworkMessage msg, ClientHandler handler) {
    try {
      int sellerId = ((Number) msg.getData()).intValue();
      List<Review> reviews = new ReviewSqlDAO().getReviewsBySeller(sellerId);
      handler.sendResponse("SUCCESS", reviews, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetItemsBySeller(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer c)) {
        throw new UserException("Phải đăng nhập!");
      }

      int sellerId = ((Number) msg.getData()).intValue();
      if (sellerId != c.getId()) {
        throw new UserException("Không được xem kho người khác!");
      }
      List<Item> items = new ItemSqlDAO().getItemsBySeller(sellerId);
      handler.sendResponse("SUCCESS", items, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetSellerAuctions(NetworkMessage msg, ClientHandler handler, String status) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer c)) {
        throw new UserException("Phải đăng nhập!");
      }
      int sellerId = ((Number) msg.getData()).intValue();
      if (sellerId != c.getId()) {
        throw new UserException("Không được xem phiên đấu giá của người khác!");
      }
      List<Auction> auctions = auctionSqlDAO.getAuctionsBySeller(sellerId, status);
      handler.sendResponse("SUCCESS", auctions, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private Item parseItemFromJson(String json) throws UserException {
    JsonObject o = gson.fromJson(json, JsonObject.class);
    String type = o.get("itemType").getAsString().toUpperCase();
    String name = o.get("name").getAsString();
    String description = o.has("description") ? o.get("description").getAsString() : "";
    BigDecimal price = readBigDecimal(o, "startingPrice");
    String imageData = "";
    if (o.has("imageBase64") && !o.get("imageBase64").isJsonNull()) {
      imageData = o.get("imageBase64").getAsString();
    }
    String city = o.has("city") ? o.get("city").getAsString() : "";

  switch (type) {
    case "ELECTRONICS" -> {
      String brand = o.has("brand") ? o.get("brand").getAsString() : "Unknown";
      int warranty = o.has("warrantyMonths") ? o.get("warrantyMonths").getAsInt() : 12;
      Electronics e = new Electronics(name, description, price, "", 0, brand, warranty);
      e.setCity(city);
      e.setImageData(imageData);
      return e;
    }
    case "ART" -> {
      String author = o.has("author") ? o.get("author").getAsString() : "Unknown";
      int year = o.has("creationYear") ? o.get("creationYear").getAsInt() : 2000;
      String material = o.has("material") ? o.get("material").getAsString() : "";
      Art a = new Art(name, description, price, "", 0, author, year, material);
      a.setCity(city);
      a.setImageData(imageData);
      return a;
    }
    case "VEHICLE" -> {
      String brand = o.has("brand") ? o.get("brand").getAsString() : "";
      String model = o.has("model") ? o.get("model").getAsString() : "";
      int year = o.has("manufacturingYear") ? o.get("manufacturingYear").getAsInt() : 2020;
      double mileage = o.has("mileage") ? o.get("mileage").getAsDouble() : 0.0;
      String engine = o.has("engineType") ? o.get("engineType").getAsString() : "4 kỳ";
      String fuel = o.has("fuelType") ? o.get("fuelType").getAsString() : "xăng";
      Vehicle v = new Vehicle(name, description, price, "", 0,
          brand, model, year, mileage, engine, fuel);
      v.setCity(city);
      v.setImageData(imageData);
      return v;
    }
    default -> throw new UserException("Loại sản phẩm không hỗ trợ: " + type);
  }
}

  private BigDecimal readBigDecimal(JsonObject o, String key) throws UserException {
    if (!o.has(key)) {
      throw new UserException("Thiếu trường: " + key);
    }
    var el = o.get(key);
    if (el.isJsonPrimitive()) {
      var p = el.getAsJsonPrimitive();
      if (p.isNumber()) return p.getAsBigDecimal();
      if (p.isString()) {
        String s = p.getAsString().trim();
        if (s.isEmpty()) throw new UserException("Giá không hợp lệ!");
        return new BigDecimal(s);
      }
    }
    throw new UserException("Giá không hợp lệ!");
  }

  private void handleRegister(String regData) throws UserException {
    String[] regParts = regData.split(" ");
    if (regParts.length < 2) throw new UserException("Vui lòng nhập đầy đủ thông tin đăng ký!");

    String plainPassword = regParts[1].trim();
    // Băm mật khẩu BCrypt
    String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(plainPassword, org.mindrot.jbcrypt.BCrypt.gensalt(12));

    Customer newCustomer = new Customer(regParts[0].trim(), hashedPassword, BigDecimal.ZERO);
    userSqlDAO.addUser(newCustomer);
  }

  private void handleLoginLogic(String credentials) throws AuthenticationException {
    if (credentials == null || credentials.trim().isEmpty()) throw new AuthenticationException("Dữ liệu trống!");
    if (credentials.toLowerCase().contains("root"))
      throw new AuthenticationException("Tài khoản root không được phép truy cập từ Client!");
  }
}