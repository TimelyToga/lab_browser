import java.awt.Dimension;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import resources.BrowserException;


/**
 * A class used to display the viewer for a simple HTML browser.
 * 
 * See this tutorial for help on how to use the variety of components:
 *   http://download.oracle.com/otndocs/products/javafx/2/samples/Ensemble/
 * 
 * @author Owen Astrachan
 * @author Marcin Dobosz
 * @author Yuzhang Han
 * @author Edwin Ward
 * @author Robert C. Duvall
 */
public class BrowserView {
    private static final String GO_COMMAND = "GoCommand";
	public static final String HOME_COMMAND = "HomeCommand";
	public static final String NEXT_COMMAND = "NextCommand";
	private static final String BACK_COMMAND = "BackCommand";
	public static final String ERROR_TITLE = "ErrorTitle";
	public static final String SET_HOME_COMMAND = "SetHomeCommand";
	public static final String FAVORITE_PROMPT = "FavoritePrompt";
	public static final String FAVORITE_PROMPT_TITLE = "FavoritePromptTitle";
	// constants
    public static final Dimension DEFAULT_SIZE = new Dimension(800, 600);
    public static final String DEFAULT_RESOURCE_PACKAGE = "resources/";
    public static final String STYLESHEET = "default.css";
    public static final String BLANK = " ";
    
    public static final String INVALID_URL = "INVALID_URL";

    // scene, needed to report back to Application
    private Scene myScene;
    // web page
    private WebView myPage;
    // information area
    private Label myStatus;
    // navigation
    private TextField myURLDisplay;
    private Button myBackButton;
    private Button myNextButton;
    private Button myHomeButton;
    private Button myAddFavoritesButton;
    // favorites
    private ComboBox<String> myFavorites;
    // get strings from resource file
    private ResourceBundle myResources;
    // the data
    private BrowserModel myModel;

    /**
     * Create a view of the given model of a web browser.
     */
    public BrowserView (BrowserModel model, String language) {
        myModel = model;
        // use resources for labels
        myResources = ResourceBundle.getBundle(DEFAULT_RESOURCE_PACKAGE + language);
        BorderPane root = new BorderPane();
        // must be first since other panels may refer to page
        root.setCenter(makePageDisplay());
        root.setTop(makeInputPanel());
        root.setBottom(makeInformationPanel());
        // control the navigation
        enableButtons();
        // create scene to hold UI
        myScene = new Scene(root, DEFAULT_SIZE.width, DEFAULT_SIZE.height);
        //myScene.getStylesheets().add(DEFAULT_RESOURCE_PACKAGE + STYLESHEET);
    }

    /**
     * Display given URL.
     */
    public void showPage (String url) {
    	try {
            URL valid = myModel.go(url);
            update(valid);
    	}
        catch (BrowserException e){
            showError(e.getMessage());
        }
    }

    /**
     * Returns scene for this view so it can be added to stage.
     */
    public Scene getScene () {
        return myScene;
    }

    /**
     * Display given message as information in the GUI.
     */
    public void showStatus (String message) {
        myStatus.setText(message);
    }

    /**
     * Display given message as an error in the GUI.
     */
    public void showError (String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(myResources.getString(ERROR_TITLE));
        alert.setContentText(message);
        alert.showAndWait();
    }

    // move to the next URL in the history
    private void next () {
        update(myModel.next());
    }

    // move to the previous URL in the history
    private void back () {
        update(myModel.back());
    }

    // change current URL to the home page, if set
    private void home () {
        showPage(myModel.getHome().toString());
    }

    // change page to favorite choice
    private void showFavorite (String favorite) {
        showPage(myModel.getFavorite(favorite).toString());
        // reset favorites ComboBox so the same choice can be made again
    }

    // update just the view to display given URL
    private void update (URL url) {
        String urlText = url.toString();
        myPage.getEngine().load(urlText);
        myURLDisplay.setText(urlText);
        enableButtons();
    }

    // prompt user for name of favorite to add to collection
    private void addFavorite () {
        TextInputDialog input = new TextInputDialog("");
        input.setTitle(myResources.getString(FAVORITE_PROMPT_TITLE));
        input.setContentText(myResources.getString(FAVORITE_PROMPT));
        Optional<String> response = input.showAndWait();
        // did user make a choice?
        if (response.isPresent()) {
            myModel.addFavorite(response.get());
            myFavorites.getItems().add(response.get());
        }
    }

    // only enable buttons when useful to user
    private void enableButtons () {
        myBackButton.setDisable(! myModel.hasPrevious());
        myNextButton.setDisable(! myModel.hasNext());
        myHomeButton.setDisable(myModel.getHome() == null);
        //TODO: disable favorites button if curr page already in fav
    }

    // convenience method to create HTML page display
    private Node makePageDisplay () {
        myPage = new WebView();
        // catch "browsing" events within web page
        myPage.getEngine().getLoadWorker().stateProperty().addListener(new LinkListener());
        return myPage;
    }

    // organize user's options for controlling/giving input to model
    private Node makeInputPanel () {
        VBox result = new VBox();
        result.getChildren().addAll(makeNavigationPanel(), makePreferencesPanel());
        return result;
    }

    // make the panel where "would-be" clicked URL is displayed
    private Node makeInformationPanel () {
        // BLANK must be non-empty or status label will not be displayed in GUI
        myStatus = new Label(BLANK);
        return myStatus;
    }

    // make user-entered URL/text field and back/next buttons
    private Node makeNavigationPanel () {
        HBox result = new HBox();
        // create buttons, with their associated actions
        // old style way to do set up callback (anonymous class)
        myBackButton = makeButton(BACK_COMMAND, new EventHandler<ActionEvent>() {
            @Override      
            public void handle (ActionEvent event) {       
                back();        
            }      
        });
        result.getChildren().add(myBackButton);
        // new style way to do set up callback (lambdas)
        myNextButton = makeButton(NEXT_COMMAND, event -> next());
        result.getChildren().add(myNextButton);
        myHomeButton = makeButton(HOME_COMMAND, event -> home());
        result.getChildren().add(myHomeButton);
        myAddFavoritesButton = makeButton(FAVORITE_PROMPT_TITLE, event -> addFavorite());
        result.getChildren().add(myAddFavoritesButton);
        // if user presses button or enter in text field, load/show the URL
        EventHandler<ActionEvent> showHandler = new ShowPage();
        result.getChildren().add(makeButton(GO_COMMAND, showHandler));
        myURLDisplay = makeInputField(40, showHandler);
        result.getChildren().add(myURLDisplay);
        return result;
    }

    // make buttons for setting favorites/home URLs
    private Node makePreferencesPanel () {
        HBox result = new HBox();
        myFavorites = new ComboBox<String>();
    	myFavorites.getItems().addAll(myModel.getFavoriteKeys());
    	myFavorites.valueProperty().addListener(new ChangeListener<String>(){

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				showFavorite(newValue);
			}
    		
    	});
    	
        result.getChildren().addAll(makeButton(SET_HOME_COMMAND, event -> {
            myModel.setHome();
            enableButtons();
        }), myFavorites);
        return result;
    }

    // makes a button using either an image or a label
    private Button makeButton (String property, EventHandler<ActionEvent> handler) {
        // represent all supported image suffixes
        final String IMAGEFILE_SUFFIXES = 
            String.format(".*\\.(%s)", String.join("|", ImageIO.getReaderFileSuffixes()));

        Button result = new Button();
        String label = myResources.getString(property);
        if (label.matches(IMAGEFILE_SUFFIXES)) {
            result.setGraphic(new ImageView(
                new Image(getClass().getResourceAsStream(DEFAULT_RESOURCE_PACKAGE + label))));
        } else {
            result.setText(label);
        }
        result.setOnAction(handler);
        return result;
    }

    // make text field for input
    private TextField makeInputField (int width, EventHandler<ActionEvent> handler) {
        TextField result = new TextField();
        result.setPrefColumnCount(width);
        result.setOnAction(handler);
        return result;
    }

    // display page
    // very old style way create a callback (inner class)
    private class ShowPage implements EventHandler<ActionEvent> {
        @Override      
        public void handle (ActionEvent event) {       
            showPage(myURLDisplay.getText());      
        }      
    }


    // Inner class to deal with link-clicks and mouse-overs Mostly taken from
    //   http://blogs.kiyut.com/tonny/2013/07/30/javafx-webview-addhyperlinklistener/
    private class LinkListener implements ChangeListener<State> {
        public static final String EVENT_CLICK = "click";
        public static final String EVENT_MOUSEOVER = "mouseover";
        public static final String EVENT_MOUSEOUT = "mouseout";

        @Override
        public void changed (ObservableValue<? extends State> ov, State oldState, State newState) {
            if (newState == Worker.State.SUCCEEDED) {
                EventListener listener = event -> {
                    final String href = ((Element)event.getTarget()).getAttribute("href");
                    if (href != null) {
                        String domEventType = event.getType();
                        if (domEventType.equals(EVENT_CLICK)) {
                            showPage(href);
                        } else if (domEventType.equals(EVENT_MOUSEOVER)) {
                            showStatus(href);
                        } else if (domEventType.equals(EVENT_MOUSEOUT)) {
                            showStatus(BLANK);
                        }
                    }
                };
                Document doc = myPage.getEngine().getDocument();
                NodeList nodes = doc.getElementsByTagName("a");
                for (int i = 0; i < nodes.getLength(); i++) {
                    EventTarget node = (EventTarget)nodes.item(i);
                    node.addEventListener(EVENT_CLICK, listener, false);
                    node.addEventListener(EVENT_MOUSEOVER, listener, false);
                    node.addEventListener(EVENT_MOUSEOUT, listener, false);
                }
            }
        }
    };
    
}
