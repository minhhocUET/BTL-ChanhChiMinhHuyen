package com.uet.bidding.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.dao.*;
import com.uet.bidding.exception.AuthenticationException;
import com.uet.bidding.exception.InvalidBidException;
import com.uet.bidding.exception.UserException;
import com.uet.bidding.model.*;
import com.uet.bidding.service.AuctionManager;

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
        case "CHANGE_PASSWORD" -> handleChangePassword(msg, handler); // 🚀 ĐÃ BỔ SUNG CASE NÀY
        case "ADD_BALANCE" -> handleAddBalance(msg, handler);
        case "BID" -> handleBid(msg, handler);
        case "GET_BID_HISTORY" -> handleGetBidHistory(msg, handler);
        case "GET_ALL_AUCTIONS" -> {
          try {
            // 1. Lấy danh sách từ Database
            List<Auction> runningAuctions = auctionSqlDAO.getRunningAuctionsForHall();

            // 2. 🎯 Cực kỳ quan trọng: Lặp qua và lấy số lượng đăng ký thực tế gán vào object
            for (Auction a : runningAuctions) {
              a.setRegisteredCount(auctionSqlDAO.getRegistrationCount(a.getId()));
            }

            // 3. Trả về Client
            handler.sendResponse("SUCCESS", runningAuctions, reqId);
          } catch (Exception e) {
            handler.sendResponse("ERROR", "Lỗi tải sảnh đấu giá: " + e.getMessage(), reqId);
          }
        }        case "CREATE_AUCTION" -> handleCreateAuction(msg, handler);
        case "SELLER_END_AUCTION" -> handleSellerEndAuction(msg, handler);
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
        case "GET_BIDDER_ACTIVE_AUCTIONS" -> handleGetBidderActiveAuctions(msg, handler);
        case "GET_BIDDER_HISTORY" -> handleGetBidderHistory(msg, handler);
        case "IS_REGISTERED_FOR_AUCTION" -> handleIsRegisteredForAuction(msg, handler);

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

        case "DELETE_ITEM" -> {
          try {
            // 1. Ép kiểu dữ liệu ID lấy từ gói tin Gson
            int deleteItemId = ((Number) msg.getData()).intValue();
            System.out.println("⚙️ [Server] Seller yêu cầu xóa sản phẩm ID: " + deleteItemId);

            // 2. Thực hiện xóa dữ liệu và dọn dẹp file vật lý trên Cloud
            boolean deleteSuccess = itemSqlDAO.deleteItemCompletely(deleteItemId);

            // 3. 🎯 SỬA CHỖ NÀY: Dùng sendResponse để truyền trả reqId về cho Client nhận diện
            if (deleteSuccess) {
              handler.sendResponse("DELETE_ITEM_SUCCESS", "Xóa sản phẩm thành công!", reqId);
              System.out.println("✅ [Server] Đã dọn dẹp sạch sẽ DB và ảnh của sản phẩm #" + deleteItemId);
            } else {
              handler.sendResponse("DELETE_ITEM_FAILED", "Không tìm thấy sản phẩm hoặc sản phẩm không hợp lệ!", reqId);
              System.out.println("⚠️ [Server] Xóa thất bại, sản phẩm #" + deleteItemId + " không tồn tại.");
            }
          } catch (Exception e) {
            System.err.println("❌ [Server] Lỗi Server khi xóa Item: " + e.getMessage());
            handler.sendResponse("DELETE_ITEM_FAILED", "Lỗi Server: " + e.getMessage(), reqId);
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

        // 🎯 ĐOẠN CODE ĐÃ ĐƯỢC FIX HẾT LỖI BÁO ĐỎ:
        case "CHECK_AUTO_BID" -> {
          try {
            int auctionId = ((Number) msg.getData()).intValue();
            if (handler.getLoggedInUser() != null) {
              int bidderId = handler.getLoggedInUser().getId();
              BigDecimal maxBid = auctionSqlDAO.getActiveAutoBidMaxPrice(auctionId, bidderId);

              // 🎯 ĐỔI THÀNH TYPE RIÊNG BIỆT THEO ĐÚNG CONVENTION DỰ ÁN CỦA BẠN
              if (maxBid != null) {
                handler.sendResponse("CHECK_AUTO_BID_SUCCESS", maxBid.toString(), reqId);
              } else {
                handler.sendResponse("CHECK_AUTO_BID_SUCCESS", null, reqId);
              }
            } else {
              handler.sendResponse("ERROR", "Yêu cầu đăng nhập trước!", reqId);
            }
          } catch (Exception e) {
            handler.sendResponse("ERROR", "Lỗi kiểm tra AutoBid: " + e.getMessage(), reqId);
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
  // --- CÁC HÀM XỬ LÝ CHI TIẾT ĐƯỢC VIẾT THÊM VÀO PHÍA DƯỚI ---

  /**
   * 🚀 HÀM MỚI: Xử lý đổi mật khẩu
   */
  private void handleChangePassword(NetworkMessage msg, ClientHandler handler) {
    try {
      if (handler.getLoggedInUser() == null) {
        handler.sendResponse("ERROR", "Bạn chưa đăng nhập!", msg.getRequestId());
        return;
      }

      int userId = handler.getLoggedInUser().getId();

      String payload = String.valueOf(msg.getData());
      String[] parts = payload.split("\\|", -1);

      String oldPass = parts.length > 0 ? parts[0] : "";
      String newPass = parts.length > 1 ? parts[1] : "";
      String confirmPass = parts.length > 2 ? parts[2] : "";

      // 🔥 ƯU TIÊN TUYỆT ĐỐI SỐ 1: Kiểm tra mật khẩu cũ trước tiên
      User currentUser = userSqlDAO.findById(userId);
      String storedHash = currentUser.getPassword();

      if (!org.mindrot.jbcrypt.BCrypt.checkpw(oldPass, storedHash)) {
        handler.sendResponse("ERROR", "Mật khẩu cũ không chính xác!", msg.getRequestId());
        return; // Cắt đuôi tại đây, sai mật khẩu cũ là dừng luôn, không quan tâm các ô khác nhập gì!
      }

      // 🎯 CHỈ KHI MẬT KHẨU CŨ CHÍNH XÁC - MỚI TÍNH TIẾP CÁC BƯỚC DƯỚI ĐÂY:

      // Kiểm tra không trùng khớp
      if (!newPass.equals(confirmPass)) {
        handler.sendResponse("ERROR", "Mật khẩu mới và nhập lại mật khẩu không khớp!", msg.getRequestId());
        return;
      }

      // Kiểm tra mật khẩu mới trùng mật khẩu cũ
      if (oldPass.equals(newPass)) {
        handler.sendResponse("ERROR", "Mật khẩu mới phải khác với mật khẩu hiện tại!", msg.getRequestId());
        return;
      }

      // Tiến hành cập nhật
      userSqlDAO.updatePassword(userId, oldPass, newPass);
      handler.sendResponse("SUCCESS", "Đổi mật khẩu thành công!", msg.getRequestId());

    } catch (com.uet.bidding.exception.UserException e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", "Lỗi Server: " + e.getMessage(), msg.getRequestId());
      e.printStackTrace();
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
        handler.sendResponse("APPROVE_SUCCESS", "Đã duyệt sản phẩm thành công!", msg.getRequestId());

        // Thông báo cho Seller biết hàng của họ đã được duyệt (Tùy chọn)
        Server.broadcast(new NetworkMessage("BROADCAST",
            "Sản phẩm #" + approveId + " đã được Admin phê duyệt. Có thể đem đấu giá!"));
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

      // 1. Lấy Base64 từ Client gửi lên và đẩy thẳng lên Cloudinary
      if (itemJson.has("imageBase64") && !itemJson.get("imageBase64").isJsonNull()) {
        String base64Data = itemJson.get("imageBase64").getAsString();
        System.out.println("👉 [DEBUG 1] Đã nhận Base64, độ dài: " + base64Data.length());

        // --- BẮT BỆNH 2: Cloudinary có upload thành công và trả về Link không? ---
        imagePath = com.uet.bidding.util.CloudinaryUtil.uploadFromBase64(base64Data);
        System.out.println("👉 [DEBUG 2] Link Cloudinary trả về: " + imagePath);
      } else {
      System.out.println("❌ [DEBUG 1] THẤT BẠI: Server KHÔNG tìm thấy trường 'imageBase64'!");
      }

      // 2. Parse Item nhưng KHÔNG gán Base64 vào object Item
      Item item = parseItemFromJson(itemJson.toString());
      item.setSellerId(c.getId());
      // 3. Gán link mạng (https://...) vào Database thay vì đường dẫn vật lý
      item.setImagePath(imagePath);
      item.setImageData(null);
      System.out.println("👉 [DEBUG 3] imagePath chuẩn bị đẩy vào DAO: " + item.getImagePath());

      itemSqlDAO.addItem(item); // Giả sử bạn đã đổi DAO để nhận image_path

      // 🚀 BỔ SUNG DÒNG NÀY: Phát tín hiệu Real-time cho toàn hệ thống (Admin sẽ bắt được case này)
      Server.broadcast(new NetworkMessage("SERVER_BROADCAST_NEW_ITEM", item));
      System.out.println("📢 [Server] Đã phát tín hiệu sản phẩm mới: " + item.getName());

      handler.sendResponse("SUCCESS", "Đã gửi yêu cầu phê duyệt!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private String saveImageToCloud(String base64Data) throws IOException {
    if (base64Data == null || base64Data.isEmpty()) return null;

    // Gọi đến Utility Cloudinary chúng ta đã viết
    // Nó sẽ trả về link https://res.cloudinary.com/...
    return com.uet.bidding.util.CloudinaryUtil.uploadFromBase64(base64Data);
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
        updated.setRegisteredCount(auctionSqlDAO.getRegistrationCount(auctionId));

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
      if (parts.length < 4) { // Yêu cầu đủ 4 tham số: id, giá, phút, bước giá
        throw new UserException("Sai cú pháp! Gửi: itemId startPrice durationMinutes bidIncrement");
      }

      int itemId = Integer.parseInt(parts[0]);
      BigDecimal startPrice = new BigDecimal(parts[1]);
      int durationMinutes = Integer.parseInt(parts[2]);
      BigDecimal bidIncrement = new BigDecimal(parts[3]);

      // Validation
      if (durationMinutes <= 0) throw new UserException("Thời lượng phải > 0 phút!");
      if (startPrice.compareTo(BigDecimal.ZERO) <= 0) throw new UserException("Giá khởi điểm phải > 0!");
      if (bidIncrement.compareTo(BigDecimal.ZERO) <= 0) throw new UserException("Bước giá phải > 0!");

      Item item = new ItemSqlDAO().findById(itemId);
      if (item == null) throw new UserException("Không tìm thấy sản phẩm!");
      if (item.getSellerId() != seller.getId()) throw new UserException("Sản phẩm không thuộc sở hữu của bạn!");
      if (item.isInAuction()) throw new UserException("Sản phẩm đang trong phiên đấu giá khác!");

      // Tính EndTime
      LocalDateTime endTime = LocalDateTime.now().plusMinutes(durationMinutes);

      // 🎯 THAY ĐỔI QUAN TRỌNG: Truyền đầy đủ tham số vào Manager
      // Không set lẻ tẻ ở ngoài nữa
      Auction created = AuctionManager.getInstance().createAuction(item, startPrice, endTime, bidIncrement);

      // Thông báo thành công
      Server.broadcast(new NetworkMessage("NEW_AUCTION_ADDED", created));
      Server.broadcast(new NetworkMessage("AUCTION_UPDATED", created));

      handler.sendResponse("SUCCESS", created, msg.getRequestId());

      System.out.println("[Server] Đã tạo phiên #" + created.getId() + " với bước giá " + bidIncrement);

    } catch (Exception e) {
      e.printStackTrace();
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleSellerEndAuction(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer seller)) {
        throw new UserException("Phải đăng nhập bằng tài khoản người bán!");
      }
      int auctionId = ((Number) msg.getData()).intValue();
      Auction auction = auctionSqlDAO.findById(auctionId);
      if (auction.getItem() == null || auction.getItem().getSellerId() != seller.getId()) {
        throw new UserException("Bạn chỉ được dừng phiên do chính mình tạo!");
      }
      if ("FINISHED".equals(auction.getStatus()) || "CANCELED".equals(auction.getStatus())) {
        throw new UserException("Phiên đã kết thúc hoặc đã hủy!");
      }
      auctionSqlDAO.finishAuction(auctionId);
      AuctionManager.getInstance().refreshAuctionFromDb(auctionId);
      Auction updated = auctionSqlDAO.findById(auctionId);
      updated.setRegisteredCount(auctionSqlDAO.getRegistrationCount(auctionId));
      Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
      handler.sendResponse("SUCCESS", updated, msg.getRequestId());
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
      int auctionId = ((Number) msg.getData()).intValue();
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
      // 1. Lưu xuống Database (Đảm bảo hàm này trong DAO có thực thi INSERT)
      auctionSqlDAO.registerBidderForAuction(auctionId, customer.getId());

      // 2. Lấy con số chính xác vừa được lưu trong Database ra
      int newCount = auctionSqlDAO.getRegistrationCount(auctionId);

      // 3. 🎯 ĐỒNG BỘ VÀO RAM: Bắt buộc để những người dùng khác khi gọi AuctionManager nhận được số đúng
      Auction cachedAuction = AuctionManager.getInstance().getAuction(auctionId);
      if (cachedAuction != null) {
        cachedAuction.setRegisteredCount(newCount);
      }

      // 4. Trả phản hồi về cho người vừa đăng ký
      Auction updated = auctionSqlDAO.findById(auctionId);
      updated.setRegisteredCount(newCount);

      Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
      handler.sendResponse("SUCCESS", updated, msg.getRequestId());

      System.out.println("✅ [Server] User " + customer.getUsername() + " đã đăng ký phiên #" + auctionId + ". Tổng số: " + newCount);

    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetMyRegistrations(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      List<Auction> auctions = auctionSqlDAO.getFastActiveAuctionsForBidder(customer.getId());
      handler.sendResponse("SUCCESS", auctions, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetBidderActiveAuctions(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      int bidderId = ((Number) msg.getData()).intValue();
      if (bidderId != customer.getId()) {
        throw new UserException("Không được xem danh sách của người khác!");
      }

      BidSqlDAO bidDao = new BidSqlDAO();
      // 🌟 GỌI ĐÚNG: Sử dụng hàm lấy siêu tốc đã gom nhóm dữ liệu
      List<Auction> auctions = auctionSqlDAO.getFastActiveAuctionsForBidder(bidderId);

      List<java.util.Map<String, Object>> payload = new java.util.ArrayList<>();
      for (Auction a : auctions) {
        // ✅ ĐÃ XÓA dòng N+1 Query (getRegistrationCount) cũ vì SQL Fast đã lo việc này
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("auction", a);
        row.put("myHighestBid", bidDao.getMaxBidByBidder(a.getId(), bidderId));
        payload.add(row);
      }
      handler.sendResponse("SUCCESS", payload, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetBidderHistory(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      int bidderId = ((Number) msg.getData()).intValue();
      if (bidderId != customer.getId()) {
        throw new UserException("Không được xem lịch sử của người khác!");
      }

      // 🌟 THÊM DÒNG NÀY: Giả lập mạng chậm 1.2 giây để quan sát chữ "Đang tải dữ liệu..." trên UI
      try { Thread.sleep(1200); } catch (InterruptedException ignored) {}

      ReviewSqlDAO reviewDao = new ReviewSqlDAO();
      BidSqlDAO bidDao = new BidSqlDAO();
      // 🌟 GỌI ĐÚNG: Sử dụng hàm lấy siêu tốc đã gom nhóm dữ liệu
      List<Auction> auctions = auctionSqlDAO.getFastFinishedAuctionsForBidder(bidderId);

      List<java.util.Map<String, Object>> payload = new java.util.ArrayList<>();
      for (Auction a : auctions) {
        // ✅ ĐÃ XÓA dòng N+1 Query (getRegistrationCount) cũ giúp tối ưu tốc độ tuyệt đối
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("auction", a);
        row.put("reviewed", reviewDao.hasReviewForAuction(a.getId(), bidderId));

        BigDecimal myBid = bidDao.getMaxBidByBidder(a.getId(), bidderId);
        row.put("myHighestBid", myBid);
        payload.add(row);
      }
      handler.sendResponse("SUCCESS", payload, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleIsRegisteredForAuction(NetworkMessage msg, ClientHandler handler) {
    try {
      if (!(handler.getLoggedInUser() instanceof Customer customer)) {
        throw new UserException("Phải đăng nhập!");
      }
      int auctionId = ((Number) msg.getData()).intValue();
      boolean registered = auctionSqlDAO.isBidderRegistered(auctionId, customer.getId());
      handler.sendResponse("SUCCESS", registered, msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetBidHistory(NetworkMessage msg, ClientHandler handler) {
    try {
      int auctionId = ((Number) msg.getData()).intValue();
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
      AuctionManager.getInstance().syncAutoBidCache(auctionId);
      Auction updated = auctionSqlDAO.findById(auctionId);
      updated.setRegisteredCount(auctionSqlDAO.getRegistrationCount(auctionId));
      Server.broadcast(new NetworkMessage("AUCTION_UPDATED", updated));
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
      int auctionId = ((Number) msg.getData()).intValue();
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

      // 1. Kiểm tra phiên đấu giá có tồn tại không (BỔ SUNG)
      Auction auction = auctionSqlDAO.findById(auctionId);
      if (auction == null) {
        throw new UserException("Phiên đấu giá không tồn tại hoặc đã bị xóa!");
      }

      if (!"FINISHED".equals(auction.getStatus())) {
        throw new UserException("Chỉ được đánh giá sau khi phiên đã kết thúc!");
      }
      if (auction.getHighestBidder() == null
          || auction.getHighestBidder().getId() != customer.getId()) {
        throw new UserException("Chỉ người thắng đấu giá mới được đánh giá!");
      }

      // 2. Kiểm tra xem đã từng đánh giá phiên này chưa (BỔ SUNG)
      ReviewSqlDAO reviewSqlDAO = new ReviewSqlDAO();
      if (reviewSqlDAO.hasReviewForAuction(auctionId, customer.getId())) {
        throw new UserException("Bạn đã đánh giá phiên đấu giá này rồi!");
      }

      // Tiến hành thêm mới sau khi mọi điều kiện đã thỏa mãn hoàn toàn
      reviewSqlDAO.addReview(auctionId, sellerId, customer.getId(), stars, comment);
      handler.sendResponse("SUCCESS", "Cảm ơn bạn đã đánh giá!", msg.getRequestId());
    } catch (Exception e) {
      handler.sendResponse("ERROR", e.getMessage(), msg.getRequestId());
    }
  }

  private void handleGetReviewsBySeller(NetworkMessage msg, ClientHandler handler) {
    try {
      int sellerId = ((Number) msg.getData()).intValue();

      // 1. Lấy danh sách review từ DB (đã nạp đủ thông tin reviewer và ngày giờ nhờ ReviewSqlDAO)
      List<Review> reviews = new ReviewSqlDAO().getReviewsBySeller(sellerId);

      // 2. Khởi tạo các DAO để truy vấn thông tin Shop và Sản phẩm
      com.uet.bidding.dao.UserSqlDAO userDAO = new com.uet.bidding.dao.UserSqlDAO();
      com.uet.bidding.dao.AuctionSqlDAO auctionDAO = new com.uet.bidding.dao.AuctionSqlDAO(); // 🌟 DAO để lấy tên sản phẩm

      // 3. Tìm thông tin Tên Shop và Mô tả Shop thực tế
      String storeName = "Cửa hàng #" + sellerId; // Tên mặc định nếu không tìm thấy
      String storeDescription = "Chưa có mô tả cho cửa hàng này.";

      try {
        com.uet.bidding.model.User sellerUser = userDAO.findById(sellerId);
        if (sellerUser instanceof com.uet.bidding.model.Customer seller) {
          if (seller.getSellerProfile() != null) {
            String dbStoreName = seller.getSellerProfile().getStoreName();
            String dbDesc = seller.getSellerProfile().getDescription();

            if (dbStoreName != null && !dbStoreName.trim().isEmpty() && !"-".equals(dbStoreName)) {
              storeName = dbStoreName;
            }
            // 🎯 ĐÃ SỬA: Kiểm tra kỹ chuỗi mô tả từ Database
            if (dbDesc != null && !dbDesc.trim().isEmpty() && !"-".equals(dbDesc)) {
              storeDescription = dbDesc;
            }
          }
        }
      } catch (Exception ex) {
        System.err.println("❌ Lỗi lấy thông tin Shop từ UserSqlDAO: " + ex.getMessage());
      }

      // 4. Tạo JsonObject tổng thể để bọc tất cả dữ liệu gửi về Client
      com.google.gson.JsonObject responseData = new com.google.gson.JsonObject();
      responseData.addProperty("storeName", storeName);
      responseData.addProperty("storeDescription", storeDescription);

      // 5. Duyệt danh sách review và đóng gói dữ liệu phẳng
      com.google.gson.JsonArray richReviewsArray = new com.google.gson.JsonArray();

      for (Review r : reviews) {
        com.google.gson.JsonObject reviewJson = new com.google.gson.JsonObject();
        reviewJson.addProperty("id", r.getId());
        reviewJson.addProperty("stars", r.getStars());
        reviewJson.addProperty("comment", r.getComment());
        reviewJson.addProperty("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");

        // 🎯 ĐÃ SỬA: Logic tìm tên sản phẩm thực tế từ Database thông qua AuctionSqlDAO
        String realProductName = "Sản phẩm đấu giá"; // Giá trị dự phòng mặc định
        try {
          // Lấy Id cuộc đấu giá/sản phẩm từ review.
          // 💡 LƯU Ý: Nếu trong Model Review của bạn đặt tên hàm là getAuctionId() hoặc getItemId() thì bạn đổi lại cho đúng nhé!
          int auctionId = r.getAuctionId();

          com.uet.bidding.model.Auction auction = auctionDAO.findById(auctionId);
          if (auction != null && auction.getItem() != null) {
            String itemName = auction.getItem().getName();
            if (itemName != null && !itemName.trim().isEmpty()) {
              realProductName = itemName;
            }
          }
        } catch (Exception ex) {
          System.err.println("❌ Lỗi khi lấy tên sản phẩm cho Review #" + r.getId() + ": " + ex.getMessage());
        }

        // Đút tên sản phẩm thật tìm được vào JSON gửi về Client
        reviewJson.addProperty("productName", realProductName);

        // Tên người đánh giá thật từ r.getReviewerName()
        String reviewerName = "Người dùng ẩn danh";
        if (r.getReviewerName() != null && !r.getReviewerName().trim().isEmpty() && !"-".equals(r.getReviewerName())) {
          reviewerName = r.getReviewerName();
        } else if (r.getReviewer() != null) {
          if (r.getReviewer().getFullName() != null && !r.getReviewer().getFullName().trim().isEmpty() && !"-".equals(r.getReviewer().getFullName())) {
            reviewerName = r.getReviewer().getFullName();
          } else {
            reviewerName = r.getReviewer().getUsername();
          }
        }
        reviewJson.addProperty("reviewerName", reviewerName);

        richReviewsArray.add(reviewJson);
      }

      // Đút mảng reviews vào Object tổng thể
      responseData.add("reviews", richReviewsArray);

      // Gửi Object lớn chứa đầy đủ (storeName, storeDescription, reviews) về Client
      handler.sendResponse("SUCCESS", responseData, msg.getRequestId());

    } catch (Exception e) {
      e.printStackTrace();
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