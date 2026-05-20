package com.uet.bidding.util;

import com.uet.bidding.model.Item;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public final class ImageUtil {
  private ImageUtil() {}

  public static void loadItemImage(ImageView view, Item item) {
    if (view == null || item == null) return;
    String data = item.getImageData();
    if (data == null || data.isBlank()) {
      view.setImage(null);
      return;
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(data);
      view.setImage(new Image(new ByteArrayInputStream(bytes)));
    } catch (IllegalArgumentException e) {
      view.setImage(null);
    }
  }
}
