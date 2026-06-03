package com.uet.bidding.controller.admin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uet.bidding.network.ClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminOverviewController {

  @FXML
  private Label txtTotalUsers;
  @FXML
  private Label txtActiveAuctions;
  @FXML
  private Label txtPendingItems;

  @FXML
  public void initialize() {
    txtTotalUsers.setText("...");
    txtActiveAuctions.setText("...");
    txtPendingItems.setText("...");
    requestStatsFromServer();
  }

  private void requestStatsFromServer() {
    ClientService.getInstance().sendRequest("GET_SYSTEM_STATS", "")
        .thenAccept(response -> {
          if ("GET_SYSTEM_STATS_SUCCESS".equals(response.getType())) {
            Gson gson = ClientService.getInstance().getGson();
            String json = gson.toJson(response.getData());
            JsonObject stats = gson.fromJson(json, JsonObject.class);

            int totalUsers = stats.get("totalUsers").getAsInt();
            int activeAuctions = stats.get("activeAuctions").getAsInt();
            int pendingItems = stats.get("pendingItems").getAsInt();

            Platform.runLater(() -> {
              txtTotalUsers.setText(String.valueOf(totalUsers));
              txtActiveAuctions.setText(String.valueOf(activeAuctions));
              txtPendingItems.setText(String.valueOf(pendingItems));
            });
          }
        }).exceptionally(ex -> {
          ex.printStackTrace();
          return null;
        });
  }

  // ─── PHƯƠNG THỨC GETTER BỔ SUNG PHỤC VỤ KIỂM THỬ ────────────────

  public Label getTxtTotalUsers() {
    return txtTotalUsers;
  }

  public Label getTxtActiveAuctions() {
    return txtActiveAuctions;
  }

  public Label getTxtPendingItems() {
    return txtPendingItems;
  }

}