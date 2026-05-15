package com.uet.bidding.model;

public class NetworkMessage {

  private String type;
  private Object data;

  // ⭐ THÊM: Chỉ cần 4 dòng này!
  private String requestId;

  public NetworkMessage(String type, Object data) {
    this.type = type;
    this.data = data;
  }

  // ⭐ THÊM: Getters/Setters cho requestId
  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  // Các getter/setter cũ giữ nguyên
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

  @Override
  public String toString() {
    return "NetworkMessage{" +
        "type='" + type + '\'' +
        ", data=" + data +
        ", requestId='" + requestId + '\'' +  // ⭐ Cập nhật toString
        '}';
  }
}