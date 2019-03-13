package net.viktorc.detroid.framework.gui.dialogs;

import java.util.concurrent.TimeUnit;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.gui.models.TimeControl;

/**
 * A dialog that allows the time controls of a chess game to be set by displaying modifiable text input fields representing the time
 * controls. When saved, the time control represented by the contents of the text input fields is returned by the {@link #showAndWait()
 * showAndWait} method.
 *
 * @author Viktor
 */
public class TimeSettingsDialog extends Dialog<TimeControl> {

  private static final String STYLE_PATH = "/gui/styles/time-settings-dialog-style.css";

  /**
   * Constructs an instance based on the specified parameters.
   *
   * @param owner The parent stage.
   * @param timeControl The current time controls.
   */
  public TimeSettingsDialog(Stage owner, TimeControl timeControl) {
    initOwner(owner);
    setDialogPane(new DialogPane() {

      @Override
      protected Node createButtonBar() {
        ButtonBar node = (ButtonBar) super.createButtonBar();
        node.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
        return node;
      }
    });
    getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
    setTitle("Time Control Settings");
    ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
    ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
    getDialogPane().getButtonTypes().setAll(cancelButtonType, saveButtonType);
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 20, 10, 10));
    Label wTimeLabel = new Label("White time:");
    Label bTimeLabel = new Label("Black time:");
    Label wTimeIncLabel = new Label("White time increment:");
    Label bTimeIncLabel = new Label("Black time increment:");
    TimeTextField wTime = new TimeTextField();
    TimeTextField bTime = new TimeTextField();
    TimeTextField wTimeInc = new TimeTextField();
    TimeTextField bTimeInc = new TimeTextField();
    wTime.setText(formatTime(timeControl.getWhiteTime()));
    bTime.setText(formatTime(timeControl.getBlackTime()));
    wTimeInc.setText(formatTime(timeControl.getWhiteInc()));
    bTimeInc.setText(formatTime(timeControl.getBlackInc()));
    ChangeListener<String> listener = (observable, oldValue, newValue) -> {
      Node saveButton = getDialogPane().lookupButton(saveButtonType);
      if (newValue.matches("^[0-9]+:[0-9]{1,2}$")) {
        try {
          parseFormattedTime(newValue);
          saveButton.setDisable(false);
        } catch (Exception e) {
          saveButton.setDisable(true);
        }
      } else {
        saveButton.setDisable(true);
      }
    };
    wTime.textProperty().addListener(listener);
    bTime.textProperty().addListener(listener);
    wTimeInc.textProperty().addListener(listener);
    bTimeInc.textProperty().addListener(listener);
    grid.add(wTimeLabel, 0, 0);
    grid.add(bTimeLabel, 0, 1);
    grid.add(wTimeIncLabel, 0, 2);
    grid.add(bTimeIncLabel, 0, 3);
    grid.add(wTime, 1, 0);
    grid.add(bTime, 1, 1);
    grid.add(wTimeInc, 1, 2);
    grid.add(bTimeInc, 1, 3);
    getDialogPane().setContent(grid);
    setResultConverter(b -> {
      if (b.getButtonData().isCancelButton()) {
        return null;
      }
      try {
        return new TimeControl(parseFormattedTime(wTime.getText()), parseFormattedTime(bTime.getText()),
            parseFormattedTime(wTimeInc.getText()), parseFormattedTime(bTimeInc.getText()));
      } catch (Exception e) {
        return null;
      }
    });
  }

  /**
   * Formats a duration specified in milliseconds to [m{2,}]:[ss].
   *
   * @param time The time in milliseconds.
   * @return The formatted time string.
   */
  private String formatTime(long time) {
    return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(time),
        TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));
  }

  /**
   * Parses a string of a certain duration formatted as [m+]:[s{1,2}].
   *
   * @param formattedTime The formatted duration string.
   * @return The duration in milliseconds.
   * @throws Exception If the format or the values (e.g. -1 or 60 seconds) are illegal.
   */
  private long parseFormattedTime(String formattedTime) throws Exception {
    String[] units = formattedTime.split(":");
    if (units.length != 2) {
      throw new Exception("Illegal time format. Expected format: [m+]:[s{1,2}]");
    }
    int minutes = Integer.parseInt(units[0]);
    int seconds = Integer.parseInt(units[1]);
    if (minutes < 0 || seconds < 0 || seconds > 59) {
      throw new Exception("Illegal time unit value. Neither minutes nor seconds can be less than 0 and seconds " +
          "cannot exceed 59");
    }
    return minutes * 60000L + seconds * 1000L;
  }

  /**
   * A text input field for durations that only accepts numeric characters and colons.
   *
   * @author Viktor
   */
  private static class TimeTextField extends TextField {

    // The allowed characters.
    private static final String REGEX = "[0-9:]*";

    @Override
    public void replaceText(int start, int end, String text) {
      if (text.matches(REGEX)) {
        super.replaceText(start, end, text);
      }
    }

    @Override
    public void replaceSelection(String text) {
      if (text.matches(REGEX)) {
        super.replaceSelection(text);
      }
    }

  }

}
