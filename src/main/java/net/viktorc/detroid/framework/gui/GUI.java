package net.viktorc.detroid.framework.gui;

import java.io.IOException;
import java.util.logging.LogManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.gui.controllers.MainController;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.validation.ControllerEngine;

/**
 * A simple GUI application controlled by a controller chess engine for playing chess games against another UCI compliant chess engine
 * instance.
 *
 * @author Viktor
 */
public final class GUI extends Application {

  private static final String TITLE = "DETROID Chess GUI";
  private static final String VIEW_PATH = "/gui/views/MainLayout.fxml";
  private static final String STYLE_PATH = "/gui/styles/main-style.css";
  private static final String ICON_PATH = "/gui/images/icon.png";

  private static ControllerEngine controllerEngine;
  private static UCIEngine searchEngine;

  private MainController controller;

  /**
   * Constructs a default instance.
   */
  public GUI() {
    if (controllerEngine == null) {
      throw new IllegalStateException("The controller engine has not been set.");
    }
    if (searchEngine == null) {
      throw new IllegalStateException("The search engine has not been set.");
    }
  }

  /**
   * Sets the chess engines powering the GUI application.
   *
   * @param controllerEngine The engine responsible for enforcing the rules and importing and exporting FEN and PGN strings.
   * @param searchEngine The engine responsible for searching the chess positions.
   */
  public static void setEngines(ControllerEngine controllerEngine, UCIEngine searchEngine) {
    GUI.controllerEngine = controllerEngine;
    GUI.searchEngine = searchEngine;
  }

  @Override
  public void init() throws Exception {
    controllerEngine.init();
    searchEngine.init();
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    // To get rid of the undesired maxCellCount INFO messages.
    LogManager.getLogManager().reset();
    FXMLLoader loader = new FXMLLoader();
    loader.setControllerFactory(param -> {
      if (param.equals(MainController.class)) {
        return new MainController(primaryStage, controllerEngine, searchEngine);
      }
      return null;
    });
    BorderPane root = loader.load(getClass().getResourceAsStream(VIEW_PATH));
    Scene scene = new Scene(root);
    scene.getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
    controller = loader.getController();
    primaryStage.setScene(scene);
    primaryStage.setTitle(TITLE);
    primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_PATH)));
    primaryStage.setResizable(false);
    primaryStage.show();

  }

  @Override
  public void stop() throws Exception {
    if (controller != null) {
      controller.close();
    }
    super.stop();
    Platform.exit();
    System.exit(0);
  }

}
