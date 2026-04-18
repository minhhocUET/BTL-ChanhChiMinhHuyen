package com.uet.bidding.model;

public enum AuctionState {
    OPEN,       // Mới tạo, chờ người tham gia
    RUNNING,    // Đang trong thời gian đấu giá
    FINISHED,   // Đã kết thúc thời gian
    PAID,       // Đã thanh toán
    CANCELED    // Bị hủy
}
