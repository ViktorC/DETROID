package net.viktorc.detroid.framework.gui.dialogs;

import java.util.Observable;
import java.util.Observer;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.uci.DebugInformation;

/**
 * An alert for displaying a console to which the search engine's debug output is printed
 *
 * @author Viktor
 */
public class ConsoleAlert extends Alert implements Observer {

  private static final String STYLE_PATH = "/gui/styles/console-dialog-style.css";

  private TextArea area;

  /**
   * Constructs an instance based on the specified parameters.
   *
   * @param owner The parent stage.
   * @param debugInfo The observable debug information of the search engine.
   */
  public ConsoleAlert(Stage owner, DebugInformation debugInfo) {
    super(AlertType.INFORMATION);
    initOwner(owner);
    getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
    setTitle("Debug Console");
    setHeaderText("The debug output of the chess search engine.");
    area = new TextArea();
    area.setEditable(false);
    getDialogPane().setContent(area);
    initModality(Modality.NONE);
    debugInfo.addObserver(this);
  }

  @Override
  public void update(Observable o, Object arg) {
    DebugInformation info = (DebugInformation) o;
    String content = info.getContent();
    Platform.runLater(() -> area.appendText(content + System.lineSeparator()));
  }

}
