package com.uet.bidding.controller.main;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class LauncherTest {

  @Test
  void testLauncherMain() {
    // Đánh chặn Main.class để khi Launcher gọi Main.main() nó sẽ không chạy thật
    try (MockedStatic<Main> mockedMain = Mockito.mockStatic(Main.class)) {
      String[] args = {"test-arg"};

      // WHEN
      Launcher.main(args);

      // THEN: Xác nhận Launcher.main đã gọi chính xác Main.main() truyền đúng tham số
      mockedMain.verify(() -> Main.main(args), Mockito.times(1));
    }
  }
}