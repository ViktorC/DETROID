package net.viktorc.detroid.framework.gui.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ErrorAlert extends Alert {

	private static final String STYLE_PATH = "../styles/dialog-style.css";
	private static final String ICON_PATH = "../images/icon.png";
	
	public ErrorAlert(Stage owner, String header, String content) {
		super(AlertType.ERROR);
		initOwner(owner);
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_PATH)));
		getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
		setTitle("Error");
		setHeaderText(header);
		setContentText(content);
	}
	
}
