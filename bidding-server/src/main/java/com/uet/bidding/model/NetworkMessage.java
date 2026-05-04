package com.uet.bidding.model;

/**
 *
 */
public class NetworkMessage {
  private String type;

  // Nội dung chi tiết của tin nhắn (thường là một Object khác đã biến thành chuỗi JSON)
  private String content;

  // Constructor khởi tạo
  public NetworkMessage(String type, String content) {
    this.type = type;
    this.content = content;
  }

  // Các Getter và Setter để thư viện Gson có thể truy cập dữ liệu
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}

