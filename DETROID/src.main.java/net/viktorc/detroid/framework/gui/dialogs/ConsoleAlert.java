package net.viktorc.detroid.framework.gui.dialogs;

import java.util.Observable;
import java.util.Observer;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.uci.DebugInformation;

public class ConsoleAlert extends Alert implements Observer {

	private static final String STYLE_PATH = "../styles/console-dialog-style.css";
	private static final String ICON_PATH = "../images/icon.png";
	
	private TextArea area;
	
	public ConsoleAlert(Stage owner, DebugInformation debugInfo) {
		super(AlertType.INFORMATION);
		initOwner(owner);
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_PATH)));
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
