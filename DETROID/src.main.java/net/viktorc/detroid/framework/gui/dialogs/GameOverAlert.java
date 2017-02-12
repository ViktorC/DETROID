package net.viktorc.detroid.framework.gui.dialogs;

import javafx.scene.control.Alert;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.control.GameState;

/**
 * An alert to display the end result of a chess game to the user.
 * 
 * @author Viktor
 *
 */
public class GameOverAlert extends Alert {

	private static final String STYLE_PATH = "/net/viktorc/detroid/framework/gui/styles/dialog-style.css";
	
	/**
	 * Constructs an instance based on the specified parameters.
	 * 
	 * @param owner The parent stage.
	 * @param state The state of the game.
	 * @param onTime Whether the game terminated due to time out.
	 */
	public GameOverAlert(Stage owner, GameState state, boolean onTime) {
		super(AlertType.INFORMATION);
		initOwner(owner);
		getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
		setTitle("Game Over");
		String header = (state == GameState.WHITE_MATES || state == GameState.UNSPECIFIED_WHITE_WIN ?
				"White wins." : state == GameState.BLACK_MATES || state == GameState.UNSPECIFIED_BLACK_WIN ?
				"Black wins." : "Draw.");
		setHeaderText(header);
		String content;
		switch (state) {
		case WHITE_MATES:
			content = "White mates.";
			break;
		case UNSPECIFIED_WHITE_WIN:
			content = onTime ? "White wins on time." : "Unspecified white victory.";
			break;
		case BLACK_MATES:
			content = "Black mates.";
			break;
		case UNSPECIFIED_BLACK_WIN:
			content = onTime ? "Black wins on time." : "Unspecified black victory.";
			break;
		case STALE_MATE:
			content = "Stale mate.";
			break;
		case DRAW_BY_INSUFFICIENT_MATERIAL:
			content = "Draw by insufficient material.";
			break;
		case DRAW_BY_3_FOLD_REPETITION:
			content = "Draw by three-fold repetition.";
			break;
		case DRAW_BY_50_MOVE_RULE:
			content = "Draw by fifty-move rule.";
			break;
		case DRAW_BY_AGREEMENT:
			content = "Draw by agreement.";
			break;
		default:
			throw new IllegalArgumentException();
		}
		setContentText(content);
	}
	
}
