package com.uet.bidding.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

  private int id;
  private int userId;
  private Integer auctionId; // có thể null (nạp/rút không liên quan phiên)
  private BigDecimal amount;
  private TransactionType type;
  private TransactionStatus status;
  private LocalDateTime createdAt;

  public Transaction() {
  }

  // Constructor cho tạo mới (chưa có id)
  public Transaction(int userId, Integer auctionId, BigDecimal amount, TransactionType type, TransactionStatus status) {
    this.userId = userId;
    this.auctionId = auctionId;
    this.amount = amount;
    this.type = type;
    this.status = status;
    this.createdAt = LocalDateTime.now();
  }

  // Getters & Setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public Integer getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(Integer auctionId) {
    this.auctionId = auctionId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public TransactionType getType() {
    return type;
  }

  public void setType(TransactionType type) {
    this.type = type;
  }

  public TransactionStatus getStatus() {
    return status;
  }

  public void setStatus(TransactionStatus status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "Transaction{" +
        "id=" + id +
        ", userId=" + userId +
        ", auctionId=" + auctionId +
        ", amount=" + amount +
        ", type='" + type + '\'' +
        ", status='" + status + '\'' +
        ", createdAt=" + createdAt +
        '}';
  }
}