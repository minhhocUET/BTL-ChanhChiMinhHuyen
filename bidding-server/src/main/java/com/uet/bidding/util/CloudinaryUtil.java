package com.uet.bidding.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;

public class CloudinaryUtil {
  // Thay bằng thông tin tài khoản Cloudinary của bạn
  private static final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
      "cloud_name", "dgnf4fodc",
      "api_key", "221448219121426",
      "api_secret", "nTs5OFL19GrMOJqhQCW-fk-wf1o",
      "secure", true
  ));

  public static String uploadFromBase64(String base64Data) {
    if (base64Data == null || base64Data.isEmpty()) return null;
    try {
      // Đẩy trực tiếp chuỗi Base64 lên Cloudinary
      String prefix = "data:image/jpeg;base64,";
      Map uploadResult = cloudinary.uploader().upload(prefix + base64Data, ObjectUtils.emptyMap());
      return (String) uploadResult.get("secure_url"); // Trả về link mạng dạng https://...
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}