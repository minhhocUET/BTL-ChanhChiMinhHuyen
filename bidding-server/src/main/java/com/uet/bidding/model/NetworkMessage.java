package com.uet.bidding.model;

public class NetworkMessage {
  private String type;
  private Object data;
  private String requestId; // Bắt buộc phải có để map với Client

  public NetworkMessage(String type, Object data) {
    this.type = type;
    this.data = data;
  }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public Object getData() { return data; }
  public void setData(Object data) { this.data = data; }

  public String getRequestId() { return requestId; }
  public void setRequestId(String requestId) { this.requestId = requestId; }

  @Override
  public String toString() {
    return "NetworkMessage{type='" + type + "', data=" + data + ", requestId='" + requestId + "'}";
  }
}