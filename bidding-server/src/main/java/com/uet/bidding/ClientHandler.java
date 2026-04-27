package com.uet.bidding;

import com.google.gson.Gson;
import com.uet.bidding.dao.DatabaseConnection;
import com.uet.bidding.dao.UserDAO;
import com.uet.bidding.model.Bidder;
import com.uet.bidding.model.NetworkMessage;
import com.uet.bidding.model.Seller;
import com.uet.bidding.model.User;

import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.sql.Connection;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Gson gson = new Gson(); // Máy tháo dỡ JSON

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
             Connection conn = DatabaseConnection.getConnection();
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {

            UserDAO userDAO = new UserDAO(conn);

            String inputLine;
            // Vòng lặp nhận tin nhắn liên tục từ Client
            while ((inputLine = in.readLine()) != null) {

                try {
                    // 1. Giải mã chuỗi JSON thành đối tượng Java (Kiến thức Tuần 6)
                    NetworkMessage msg = gson.fromJson(inputLine, NetworkMessage.class);

                    // 2. Phân loại và xử lý hành động
                    String response;
                    switch (msg.getType()) {
                        case "LOGIN":
                            //content: "username password"
                            String[] loginData = msg.getContent().split(" ");
                            String username = loginData[0];
                            String password = loginData[1];

                            User user = userDAO.checkLogin(username, password);
                            if (user != null) {
                                response = "Đăng nhập thành công! Xin chào " + user.getUsername()
                                        + " với vai trò " + user.getRole() +".";
                            } else {
                                response = "Đăng nhập thất bại! Vui lòng kiểm tra lại tên đăng nhập hoặc mật khẩu.";
                            }
                            break;
                        case "REGISTER":
                            String[] regData = msg.getContent().split(" ");
                            String usernameR = regData[0];
                            String passwordR = regData[1];
                            String role = regData[2];

                            User newUser;
                            if ("BIDDER".equalsIgnoreCase(role)) {
                                newUser = new Bidder(0, usernameR, passwordR, BigDecimal.ZERO);
                            } else {
                                newUser = new Seller(0, usernameR, passwordR, BigDecimal.ZERO);
                            }

                            userDAO.addUser(newUser);
                            response = "Đăng kí thành công! Người dùng " + usernameR + " với vai trò " + role + " đã được tạo.";
                            break;
                        case "BID":
                            response = "Bạn đã đặt giá: " + msg.getContent() + " VNĐ. Đang kiểm tra...";
                            // Sau này bạn sẽ thêm logic kiểm tra giá cao nhất ở đây
                            break;
                        case "INFO":
                            response = "Thông tin sản phẩm: Bình hoa cổ thế kỷ 18.";
                            break;
                        default:
                            response = "Hệ thống không hiểu lệnh này!";
                            break;
                    }

                    // 3. Phản hồi lại cho Client
                    out.println(response);

                } catch (Exception e) {
                    out.println("Lỗi: Định dạng tin nhắn không hợp lệ.");
                }
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối với client: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Lỗi kết nối DB hoặc socket: " + e.getMessage());
        }
        finally {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
