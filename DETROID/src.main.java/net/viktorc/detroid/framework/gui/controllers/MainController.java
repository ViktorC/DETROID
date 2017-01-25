package net.viktorc.detroid.framework.gui.controllers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.viktorc.detroid.framework.control.ControllerEngine;
import net.viktorc.detroid.framework.control.GameState;
import net.viktorc.detroid.framework.gui.dialogs.GameOverDialog;
import net.viktorc.detroid.framework.gui.dialogs.ErrorDialog;
import net.viktorc.detroid.framework.gui.dialogs.PromotionDialog;
import net.viktorc.detroid.framework.gui.util.Piece;
import net.viktorc.detroid.framework.gui.viewmodels.SearchInfoViewModel;
import net.viktorc.detroid.framework.uci.SearchInformation;
import net.viktorc.detroid.framework.uci.UCIEngine;

public final class MainController implements AutoCloseable, Observer {
	
	private static final String WHITE_SQR_STYLE_CLASS = "whiteSquare";
	private static final String BLACK_SQR_STYLE_CLASS = "blackSquare";
	private static final String SELECTED_SQR_STYLE_CLASS = "selectedSquare";
	private static final String PIECE_STYLE_CLASS = "piece";
	
	private static final long DEFAULT_TIME = 300000;
	private static final long TIMER_RESOLUTION = 100;
	
	@FXML
	private MenuItem unmakeMove;
	@FXML
	private MenuItem reset;
	@FXML
	private MenuItem pasteFen;
	@FXML
	private MenuItem pastePgn;
	@FXML
	private MenuItem copyFen;
	@FXML
	private MenuItem copyPgn;
	@FXML
	private RadioMenuItem side;
	@FXML
	private GridPane board;
	@FXML
	private TextArea moveHistory;
	@FXML
	private TextField wTime;
	@FXML
	private TextField bTime;
	@FXML
	private TableView<SearchInfoViewModel> searchStatView;
	
	private ObservableList<SearchInfoViewModel> searchStats;
	private ExecutorService executor;
	private ControllerEngine controllerEngine;
	private UCIEngine searchEngine;
	private Timer timer;
	private TimerTask task;
	private long initWhiteTime;
	private long initBlackTime;
	private long whiteTimeInc;
	private long blackTimeInc;
	private ArrayDeque<Long> whiteTimes;
	private ArrayDeque<Long> blackTimes;
	private volatile boolean doTime;
	private volatile long whiteTime;
	private volatile long blackTime;
	private volatile List<String> legalMoves;
	private volatile List<String> legalDestinations;
	private volatile String selectedSource;
	private volatile Integer selectedNodeInd;
	private volatile boolean isUserWhite;
	private volatile boolean usersTurn;
	private volatile boolean gameOn;
	private volatile boolean isReset;
	
	public MainController(ControllerEngine controllerEngine, UCIEngine searchEngine) {
		executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        });
		this.controllerEngine = controllerEngine;
		this.searchEngine = searchEngine;
		legalDestinations = new ArrayList<>();
		initWhiteTime = initBlackTime = DEFAULT_TIME;
		whiteTimeInc = blackTimeInc = 0;
		timer = new Timer();
		task = new TimerTask() {
			
			@Override
			public void run() {
				if (!doTime)
					return;
				if (MainController.this.controllerEngine.isWhitesTurn()) {
					whiteTime -= TIMER_RESOLUTION;
					if (whiteTime <= 0) {
						doTime = false;
						whiteTime = 0;
						controllerEngine.whiteForfeit();
						Platform.runLater(() -> {
							setTimeField(true);
							Alert dialog = new GameOverDialog(controllerEngine.getGameState(), true);
							dialog.show();
						});
					} else
						Platform.runLater(() -> setTimeField(true));
				} else {
					blackTime -= TIMER_RESOLUTION;
					if (blackTime <= 0) {
						doTime = false;
						blackTime = 0;
						controllerEngine.blackForfeit();
						Platform.runLater(() -> {
							setTimeField(false);
							Alert dialog = new GameOverDialog(controllerEngine.getGameState(), true);
							dialog.show();
						});
					} else
						Platform.runLater(() -> setTimeField(false));
				}
			}
		};
		whiteTimes = new ArrayDeque<>();
		blackTimes = new ArrayDeque<>();
	}
	private static String sqrIndToName(int ind) {
		int rank = ind/8;
		int file = ind%8;
		return "" + Character.toString((char) ('a' + file)) + (rank + 1);
	}
	private void setTimeField(boolean white) {
		if (white)
			wTime.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(whiteTime),
					TimeUnit.MILLISECONDS.toSeconds(whiteTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(whiteTime))));
		else
			bTime.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(blackTime),
					TimeUnit.MILLISECONDS.toSeconds(blackTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(blackTime))));
	}
	private void addPiece(Piece piece, int row, int column) {
		int nodeInd = row*8 + column + 1; // The first child is not a square.
		int sqrInd = (7 - row)*8 + column;
		String sqr = sqrIndToName(sqrInd);
		Text chessPiece = new Text("" + piece.getCode());
		chessPiece.getStyleClass().add(PIECE_STYLE_CLASS);
		chessPiece.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				if (!usersTurn || controllerEngine.getGameState() != GameState.IN_PROGRESS)
					return;
				if (!gameOn) {
					gameOn = true;
					doTime = true;
					timer.schedule(task, TIMER_RESOLUTION, TIMER_RESOLUTION);
				}
				List<Node> children = board.getChildren();
				if (isLegal(sqr)) {
					if (selectedNodeInd != null)
						children.get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					selectedSource = sqr;
					selectedNodeInd = nodeInd;
					children.get(nodeInd).getStyleClass().add(SELECTED_SQR_STYLE_CLASS);
				} else if (selectedSource != null && legalDestinations.contains(sqr)) {
					String move = selectedSource + "" + sqr;
					if (!controllerEngine.getLegalMoves().contains(move))
						move += resolvePromotion().getFENNote();
					children.get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					makeMove(move);
				}
			}
		});
		board.add(chessPiece, column, row);
		GridPane.setHalignment(chessPiece, HPos.CENTER);
		GridPane.setValignment(chessPiece, VPos.CENTER);
	}
	private void setBoard(String fen) {
		board.getChildren().removeAll(board.lookupAll("." + PIECE_STYLE_CLASS));
		String board = fen.split(" ")[0].trim();
		String[] ranks = board.split("/");
		for (int i = 0; i < 8; i++) {
			String rank = ranks[i];
			int fileInd = 0;
			for (char c : rank.toCharArray()) {
				Piece piece = Piece.getByFENNote(c);
				if (piece != null)
					addPiece(piece, i, fileInd++);
				else
					fileInd += Character.getNumericValue(c);
			}
		}
	}
	private boolean isLegal(String sqr) {
		boolean legal = false;
		for (String m : legalMoves) {
			if (m.startsWith(sqr)) {
				if (legal == false)
					legalDestinations.clear();
				legal = true;
				legalDestinations.add(m.substring(2, 4));
			}
		}
		return legal;
	}
	private Piece resolvePromotion() {
		Alert dialog = new PromotionDialog(controllerEngine.isWhitesTurn());
		Optional<ButtonType> selection = dialog.showAndWait();
		return Piece.getByUnicode(selection.get().getText().toLowerCase().charAt(0));
	}
	private void handleIllegalMove(String move) {
		Alert dialog = new ErrorDialog("Illegal engine move.",
				"The engine \"" + searchEngine.getName() + "\" proposed an illegal move (" + move + ").");
		dialog.showAndWait();
	}
	private void startSearch() {
		isReset = false;
		unmakeMove.setDisable(true);
		searchEngine.getSearchInfo().addObserver(this);
		if (!gameOn) {
			gameOn = true;
			timer.schedule(task, TIMER_RESOLUTION, TIMER_RESOLUTION);
		}
		executor.submit(() -> {
			long timeLeft = controllerEngine.isWhitesTurn() ? whiteTime : blackTime;
			long start = System.currentTimeMillis();
			String bestMove = searchEngine.search(null, null, whiteTime, blackTime, whiteTimeInc, blackTimeInc,
					null, null, null, null, null, null).getBestMove();
			if (isReset)
				return;
			doTime = false;
			timeLeft -= (System.currentTimeMillis() - start);
			if (controllerEngine.isWhitesTurn())
				whiteTime = timeLeft;
			else
				blackTime = timeLeft;
			boolean isLegal = controllerEngine.getLegalMoves().contains(bestMove);
			Platform.runLater(() -> {
				if (isLegal)
					makeMove(bestMove);
				else
					handleIllegalMove(bestMove);
			});
		});
	}
	private void setMoveHistory() {
		String pgn = controllerEngine.toPGN();
		String moveList = pgn.substring(pgn.lastIndexOf(']') + 1).trim();
		moveList = moveList.replaceAll("\n", "");
		moveHistory.setText(moveList);
	}
	private void makeMove(String move) {
		doTime = false;
		if (controllerEngine.isWhitesTurn()) {
			whiteTime += whiteTimeInc;
			whiteTimes.addFirst(whiteTime);
			Platform.runLater(() -> {
				setTimeField(true);
			});
		} else {
			blackTime += blackTimeInc;
			blackTimes.addFirst(blackTime);
			Platform.runLater(() -> {
				setTimeField(false);
			});
		}
		controllerEngine.play(move);
		searchEngine.position(controllerEngine.getStartPosition());
		List<String> moveHistList = controllerEngine.getMoveHistory();
		for (String m : moveHistList)
			searchEngine.play(m);
		setMoveHistory();
		selectedNodeInd = null;
		selectedSource = null;
		setBoard(controllerEngine.toFEN());
		legalMoves = controllerEngine.getLegalMoves();
		usersTurn = !usersTurn;
		if (controllerEngine.getGameState() != GameState.IN_PROGRESS) {
			Alert dialog = new GameOverDialog(controllerEngine.getGameState(), false);
			dialog.show();
			return;
		} else if (whiteTime <= 0) {
			whiteTime = 0;
			controllerEngine.whiteForfeit();
			Platform.runLater(() -> {
				setTimeField(true);
				Alert dialog = new GameOverDialog(controllerEngine.getGameState(), true);
				dialog.show();
			});
			return;
		} else if (blackTime <= 0) {
			blackTime = 0;
			controllerEngine.blackForfeit();
			Platform.runLater(() -> {
				setTimeField(false);
				Alert dialog = new GameOverDialog(controllerEngine.getGameState(), true);
				dialog.show();
			});
			return;
		}
		doTime = true;
		if (!usersTurn) {
			synchronized (MainController.this) {
				searchStats.clear();
			}
			startSearch();
		} else {
			if (moveHistList.size() > 1)
				unmakeMove.setDisable(false);
		}
	}
	private void setPosition(String position, boolean isPgn) {
		doTime = false;
		whiteTimes.clear();
		blackTimes.clear();
		whiteTime = initWhiteTime;
		blackTime = initBlackTime;
		whiteTimes.addFirst(whiteTime);
		blackTimes.addFirst(blackTime);
		controllerEngine.newGame();
		searchEngine.newGame();
		if (isPgn) {
			if (!controllerEngine.setGame(position))
				throw new IllegalArgumentException();
			searchEngine.position(controllerEngine.getStartPosition());
			List<String> moveHistList = controllerEngine.getMoveHistory();
			for (String m : moveHistList)
				searchEngine.play(m);
			unmakeMove.setDisable(isUserWhite != controllerEngine.isWhitesTurn() || moveHistList.size() < 2);
		} else {
			if (!controllerEngine.position(position))
				throw new IllegalArgumentException();
			searchEngine.position(position);
			unmakeMove.setDisable(true);
			controllerEngine.setPlayers(isUserWhite ? "Player" : searchEngine.getName(),
					isUserWhite ? searchEngine.getName() : "Player");
			controllerEngine.setEvent("DETROID Chess GUI Game");
		}
		usersTurn = isUserWhite == controllerEngine.isWhitesTurn();
		legalMoves = controllerEngine.getLegalMoves();
		setBoard(controllerEngine.toFEN());
		setMoveHistory();
		setTimeField(true);
		setTimeField(false);
		synchronized (MainController.this) {
			searchStats.clear();
		}
		if (controllerEngine.getGameState() != GameState.IN_PROGRESS) {
			Alert dialog = new GameOverDialog(controllerEngine.getGameState(), false);
			dialog.show();
			return;
		}
		doTime = true;
		if (!usersTurn)
			startSearch();
	}
	@FXML
	public void initialize() {
		unmakeMove.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				doTime = false;
				controllerEngine.unplayLastMove();
				controllerEngine.unplayLastMove();
				searchEngine.position(controllerEngine.getStartPosition());
				List<String> moveHistList = controllerEngine.getMoveHistory();
				for (String m : moveHistList)
					searchEngine.play(m);
				if (whiteTimes.size() >= 2)
					whiteTimes.poll();
				whiteTime = whiteTimes.peek();
				if (blackTimes.size() >= 2)
					blackTimes.poll();
				blackTime = blackTimes.peek();
				legalMoves = controllerEngine.getLegalMoves();
				if (selectedNodeInd != null) {
					board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
					selectedNodeInd = null;
					selectedSource = null;
				}
				unmakeMove.setDisable(moveHistList.size() < 2);
				if (!gameOn) {
					gameOn = true;
					timer.schedule(task, TIMER_RESOLUTION, TIMER_RESOLUTION);
				}
				Platform.runLater(() -> {
					setBoard(controllerEngine.toFEN());
					setMoveHistory();
					setTimeField(true);
					setTimeField(false);
					doTime = true;
				});
			}
		});
		side.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				Platform.runLater(() -> {
					String text = side.getText();
					side.setText("Player: White".equals(text) ? "Player: Black" : "Player: White");
				});
				doTime = false;
				isReset = true;
				searchEngine.getSearchInfo().deleteObserver(MainController.this);
				searchEngine.stop();
				isUserWhite = !isUserWhite;
				doTime = true;
				usersTurn = controllerEngine.isWhitesTurn() == isUserWhite;
				unmakeMove.setDisable(!usersTurn || controllerEngine.getMoveHistory().size() < 2);
				synchronized (MainController.this) {
					searchStats.clear();
				}
				if (!usersTurn)
					startSearch();
			}
		});
		reset.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				isReset = true;
				searchEngine.getSearchInfo().deleteObserver(MainController.this);
				searchEngine.stop();
				Platform.runLater(() -> setPosition("startpos", false));
			}
		});
		pasteFen.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				isReset = true;
				searchEngine.getSearchInfo().deleteObserver(MainController.this);
				searchEngine.stop();
				Clipboard clipboard = Clipboard.getSystemClipboard();
				String fen = clipboard.getString();
				Platform.runLater(() ->  {
					try {
						setPosition(fen, false);
					} catch (Exception e) {
						Alert dialog = new ErrorDialog("FEN parsing error.",
								"The clipboard contents were not a valid FEN string.");
						dialog.show();
					}
				});
			}
		});
		pastePgn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				isReset = true;
				searchEngine.getSearchInfo().deleteObserver(MainController.this);
				searchEngine.stop();
				Clipboard clipboard = Clipboard.getSystemClipboard();
				String pgn = clipboard.getString();
				Platform.runLater(() ->  {
					try {
						setPosition(pgn, true);
					} catch (Exception e) {
						Alert dialog = new ErrorDialog("PGN parsing error.",
								"The clipboard contents were not a valid PGN string.");
						dialog.show();
					}
				});
			}
		});
		copyFen.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
				content.put(DataFormat.PLAIN_TEXT, controllerEngine.toFEN());
				clipboard.setContent(content);
			}
		});
		copyPgn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
				content.put(DataFormat.PLAIN_TEXT, controllerEngine.toPGN());
				clipboard.setContent(content);
			}
		});
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Node n = new StackPane();
				String sqr = sqrIndToName((7 - i)*8 + j);
				boolean white = i%2 == 0 ? j%2 == 0 : j%2 == 1;
				n.getStyleClass().add(white ? WHITE_SQR_STYLE_CLASS : BLACK_SQR_STYLE_CLASS);
				n.setOnMouseClicked(new EventHandler<Event>() {

					@Override
					public void handle(Event event) {
						if (!usersTurn || controllerEngine.getGameState() != GameState.IN_PROGRESS)
							return;
						if (selectedSource != null && legalDestinations.contains(sqr)) {
							String move = selectedSource + "" + sqr;
							if (!controllerEngine.getLegalMoves().contains(move))
								move += resolvePromotion().getFENNote();
							board.getChildren().get(selectedNodeInd).getStyleClass().remove(SELECTED_SQR_STYLE_CLASS);
							makeMove(move);
						}
					}
				});
				board.add(n, j, i);
			}
		}
		searchStats = FXCollections.observableArrayList();
		searchStatView.setItems(searchStats);
		moveHistory.setWrapText(true);
		isUserWhite = true;
		side.setText("Player: White");
		side.setSelected(isUserWhite);
		setPosition("startpos", false);
	}
	@Override
	public void close() throws Exception {
		timer.cancel();
		executor.shutdown();
		searchEngine.quit();
		controllerEngine.quit();
	}
	@Override
	public synchronized void update(Observable o, Object arg) {
		SearchInformation info = (SearchInformation) o;
		String score;
		switch (info.getScoreType()) {
		case EXACT:
			score = String.format("%.2f", (double) info.getScore()/100);
			break;
		case LOWER_BOUND:
			score = String.format("%.2f+", (double) info.getScore()/100);
			break;
		case UPPER_BOUND:
			score = String.format("%.2f-", (double) info.getScore()/100);
			break;
		case MATE:
			score = info.getScore() < 0 ? "-M" + Math.abs(info.getScore()) : "M" + info.getScore();
			break;
		default:
			return;
		}
		SearchInfoViewModel stats = new SearchInfoViewModel("" + info.getDepth(), String.format("%.2f", (double) info.getTime()/1000),
				"" + info.getNodes(), "" + info.getCurrentMoveNumber(), String.join(" ", info.getPv()), score, "" + (info.getTime() == 0 ?
				0 : info.getNodes()/info.getTime()), String.format("%.2f", (double) searchEngine.getHashLoadPermill()/1000));
		Platform.runLater(() -> searchStats.add(stats));
	}
	
}
