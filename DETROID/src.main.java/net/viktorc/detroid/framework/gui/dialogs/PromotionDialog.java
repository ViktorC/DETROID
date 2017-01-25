package net.viktorc.detroid.framework.gui.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.gui.util.Piece;

public class PromotionDialog extends Alert {

	private static final String STYLE_PATH = "../styles/PromotionDialogStyle.css";
	private static final String ICON_PATH = "../images/icon.png";
	
	public PromotionDialog(boolean isWhitesTurn) {
		super(AlertType.NONE);
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_PATH)));
		getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
		setTitle("Promotion");
		setHeaderText("Select a piece to promote to.");
		ButtonType queen, rook, bishop, knight;
		if (isWhitesTurn) {
			queen = new ButtonType(Character.toString(Piece.W_QUEEN.getCode()));
			rook = new ButtonType(Character.toString(Piece.W_ROOK.getCode()));
			bishop = new ButtonType(Character.toString(Piece.W_BISHOP.getCode()));
			knight = new ButtonType(Character.toString(Piece.W_KNIGHT.getCode()));
		} else {
			queen = new ButtonType(Character.toString(Piece.B_QUEEN.getCode()));
			rook = new ButtonType(Character.toString(Piece.B_ROOK.getCode()));
			bishop = new ButtonType(Character.toString(Piece.B_BISHOP.getCode()));
			knight = new ButtonType(Character.toString(Piece.B_KNIGHT.getCode()));
		}
		getButtonTypes().setAll(queen, rook, bishop, knight);
	}

}
