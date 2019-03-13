package net.viktorc.detroid.framework.gui.dialogs;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * An alert for notifying the user of some information.
 *
 * @author Viktor
 */
public class InfoAlert extends Alert {

  private static final String STYLE_PATH = "/gui/styles/dialog-style.css";

  /**
   * Constructs an instance based on the specified parameters.
   *
   * @param owner The parent stage.
   * @param header The header text.
   * @param content The message body.
   */
  public InfoAlert(Stage owner, String header, String content) {
    super(AlertType.INFORMATION);
    initOwner(owner);
    getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
    setTitle("Information");
    setHeaderText(header);
    setContentText(content);
  }

}
