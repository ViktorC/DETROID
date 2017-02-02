package net.viktorc.detroid.framework.gui.dialogs;

import java.util.Map;
import java.util.Map.Entry;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import net.viktorc.detroid.framework.uci.Option;
import net.viktorc.detroid.framework.uci.UCIEngine;
import net.viktorc.detroid.framework.uci.Option.ButtonOption;
import net.viktorc.detroid.framework.uci.Option.CheckOption;
import net.viktorc.detroid.framework.uci.Option.ComboOption;
import net.viktorc.detroid.framework.uci.Option.SpinOption;
import net.viktorc.detroid.framework.uci.Option.StringOption;

/**
 * An alert for displaying the UCI options the search engine provides to the user in the form of 
 * GUI controls the user can interact with.
 * 
 * @author Viktor
 *
 */
public class OptionsAlert extends Alert {

	private static final String STYLE_PATH = "../styles/options-dialog-style.css";
	private static final String ICON_PATH = "../images/icon.png";
	// The colour of the feedback message at the top of the pane after an option has been set.
	private static final String SUCCESS_COLOR = "green";
	private static final String ERROR_COLOR = "red";
	
	/**
	 * Constructs an instance based on the UCI option offered by the chess engine.
	 * 
	 * @param owner The parent stage.
	 * @param engine The engine whose UCI options are to be displayed as interactive GUI controls.
	 */
	public OptionsAlert(Stage owner, UCIEngine engine) {
		super(AlertType.NONE);
		initOwner(owner);
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_PATH)));
		getDialogPane().getStylesheets().add(getClass().getResource(STYLE_PATH).toExternalForm());
		setTitle("Options");
		setHeaderText("The UCI options of the chess search engine.");
		ButtonType okButtonType = new ButtonType("Ok", ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().setAll(okButtonType);
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 20, 10, 10));
		Label feedBack = new Label();
		grid.add(feedBack, 0, 0, 3, 1);
		Map<Option<?>,Object> options = engine.getOptions();
		int i = 3;
		for (Entry<Option<?>,Object> e : options.entrySet()) {
			Option<?> option = e.getKey();
			Object value = e.getValue();
			feedBack.setAlignment(Pos.CENTER);
			feedBack.setMinWidth(100);
			String name = option.getName();
			if (option instanceof CheckOption) {
				
				Label label = new Label(name + ":");
				CheckBox field = new CheckBox();
				field.selectedProperty().set((Boolean) value);
				field.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						String val = String.valueOf(field.selectedProperty().get());
						if (engine.<Boolean>setOption((CheckOption) option, field.selectedProperty().get())) {
							feedBack.setText(name + " successfully set to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(SUCCESS_COLOR));
						} else {
							feedBack.setText("Error setting " + name + " to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(ERROR_COLOR));
						}
					}
				});
				grid.add(label, 0, i);
				grid.add(field, 1, i);
			} else if (option instanceof SpinOption) {
				Label label = new Label(name + " (" + option.getMin() + ", " + option.getMax() + "):");
				SpinTextField field = new SpinTextField();
				field.setText(value.toString());
				Button button = new Button("Set");
				button.setMinWidth(50);
				button.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						String val = field.getText();
						if (engine.<Integer>setOption((SpinOption) option, Integer.parseInt(val))) {
							feedBack.setText(name + " successfully set to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(SUCCESS_COLOR));
						} else {
							feedBack.setText("Error setting " + name + " to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(ERROR_COLOR));
						}
					}
				});
				grid.add(label, 0, i);
				grid.add(field, 1, i);
				grid.add(button, 2, i);
			} else if (option instanceof StringOption) {
				Label label = new Label(name + ":");
				TextField field = new TextField();
				field.setText(option.getDefaultValue() == null ? "" : option.getDefaultValue().toString());
				Button button = new Button("Set");
				button.setMinWidth(50);
				button.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						String val = field.getText();
						if (engine.<String>setOption((StringOption) option, val)) {
							feedBack.setText(name + " successfully set to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(SUCCESS_COLOR));
						} else {
							feedBack.setText("Error setting " + name + " to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(ERROR_COLOR));
						}
					}
				});
				grid.add(label, 0, i);
				grid.add(field, 1, i);
				grid.add(button, 2, i);
			} else if (option instanceof ComboOption) {
				Label label = new Label(name + ":");
				ComboBox<String> field = new ComboBox<>();
				ComboOption c = (ComboOption) option;
				field.getItems().addAll(c.getAllowedValues());
				field.setValue(value.toString());
				field.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						String val = field.getValue();
						if (engine.<String>setOption((ComboOption) option, val)) {
							feedBack.setText(name + " successfully set to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(SUCCESS_COLOR));
						} else {
							feedBack.setText("Error setting " + name + " to " + val + ".");
							feedBack.setTextFill(Paint.valueOf(ERROR_COLOR));
						}
					}
				});
				grid.add(label, 0, i);
				grid.add(field, 1, i);
			} else if (option instanceof ButtonOption) {
				Label label = new Label(name + ":");
				Button button = new Button(option.getName());
				button.setOnAction(new EventHandler<ActionEvent>() {
					
					@Override
					public void handle(ActionEvent event) {
						if (engine.<Object>setOption((ButtonOption) option, null)) {
							feedBack.setText(name + " successfully executed.");
							feedBack.setTextFill(Paint.valueOf(SUCCESS_COLOR));
						} else {
							feedBack.setText("Error executing " + name + ".");
							feedBack.setTextFill(Paint.valueOf(ERROR_COLOR));
						}
					}
				});
				grid.add(label, 0, i);
				grid.add(button, 1, i);
			}
			i++;
		}
		getDialogPane().setContent(grid);
	}

	/**
	 * An input text field that only accepts numeric characters.
	 * 
	 * @author Viktor
	 *
	 */
	private static class SpinTextField extends TextField {
		
		// The allowed characters.
		private static final String REGEX = "[0-9]*";
		
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
