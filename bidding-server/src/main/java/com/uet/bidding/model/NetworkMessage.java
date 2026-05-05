package com.uet.bidding.model;

import java.io.Serializable;

/**
 *.
 */
public class NetworkMessage implements Serializable {

  //ID phiên bản để đảm bảo Client và Server luôn hiểu nhau
  private static final long serialVersionUID = 1L;

  private String type;

  /**
   * THAY ĐỔI QUAN TRỌNG: Dùng Object thay vì String content.
   * Điều này cho phép bạn đặt bất cứ thứ gì vào đây:
   * Một User, một Item, hoặc thậm chí là một List<Auction>.
   */
  private Object data;

  // Constructor khởi tạo
  public NetworkMessage(String type, Object data) {
    this.type = type;
    this.data = data;
  }

  // Các Getter và Setter để thư viện Gson có thể truy cập dữ liệu
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public void setContent(Object data) {
    this.data = data;
  }


  /**
   * Tiện ích để debug nhanh nội dung tin nhắn.
   */
  @Override
  public String toString() {
    return "NetworkMessage{" +
        "type='" + type + '\'' +
        ", data=" + data +
        '}';
  }
}

