package net.viktorc.detroid.framework.gui.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * An alert for notifying the user of some information.
 * 
 * @author Viktor
 *
 */
public class InfoAlert extends Alert {

	private static final String STYLE_PATH = "../styles/dialog-style.css";
	private static final String ICON_PATH = "../images/icon.png";
	
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
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_PATH)));
		getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
		setTitle("Information");
		setHeaderText(header);
		setContentText(content);
	}
	
}
