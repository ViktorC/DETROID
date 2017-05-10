package net.viktorc.detroid.framework.gui.dialogs;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * An alert for notifying the user's in case an error occurs.
 * 
 * @author Viktor
 *
 */
public class ErrorAlert extends Alert {

	private static final String STYLE_PATH = "/net/viktorc/detroid/framework/gui/styles/dialog-style.css";
	
	/**
	 * Constructs an instance based on the specified parameters.
	 * 
	 * @param owner The parent stage.
	 * @param header The header text.
	 * @param content The message body.
	 */
	public ErrorAlert(Stage owner, String header, String content) {
		super(AlertType.ERROR);
		initOwner(owner);
		getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
		setTitle("Error");
		setHeaderText(header);
		setContentText(content);
	}
	
}
