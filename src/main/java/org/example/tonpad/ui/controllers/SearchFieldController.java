package org.example.tonpad.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchFieldController extends AbstractController
{
    @Getter
    @FXML
    private VBox searchBarVBox;

    @FXML
    public HBox searchFieldHBox;

    @FXML
    public HBox searchButtonsHBox;

    @FXML
    private TextField searchField;

    @FXML
    private Button prevHitButton;

    @FXML
    private Button nextHitButton;

    @FXML
    private TextField searchResultsField;

    private Runnable onNext, onPrev;
    private java.util.function.Consumer<String> onQueryChanged;

    @FXML
    private void initialize() {
        var debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
        searchField.textProperty().addListener((o, ov, nv) -> {
            debounce.stop();
            debounce.setOnFinished(e -> {
                if (onQueryChanged != null) onQueryChanged.accept(nv.trim());
            });
            debounce.playFromStart();
        });

        searchField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            var code = e.getCode();
            if ((code == javafx.scene.input.KeyCode.F3 && e.isShiftDown()) || code == javafx.scene.input.KeyCode.UP) {
                e.consume();
                if (onPrev != null) onPrev.run();
            } else if (code == javafx.scene.input.KeyCode.F3
                    || code == javafx.scene.input.KeyCode.DOWN
                    || code == javafx.scene.input.KeyCode.ENTER) {
                e.consume();
                if (onNext != null) onNext.run();
            }
        });

        prevHitButton.setOnAction(e -> { if (onPrev != null) onPrev.run(); });
        nextHitButton.setOnAction(e -> { if (onNext != null) onNext.run(); });
    }


    public void focus() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/search-bar.fxml";
    }

    public void setOnQueryChanged(java.util.function.Consumer<String> cb){ this.onQueryChanged = cb; }
    public void setOnNext(Runnable r){ this.onNext = r; }
    public void setOnPrev(Runnable r){ this.onPrev = r; }

    public void clearResults()
    {
        searchResultsField.setText("");
    }
    public void setResults(int current1based, int total){
        searchResultsField.setText(total <= 0 ? "" : (current1based + "/" + total));
    }
    public void setQuery(String q){ searchField.setText(q == null ? "" : q); }
    public String getQuery(){ return searchField.getText().trim(); }

    public void init(AnchorPane parent) {
        parent.getChildren().add(searchBarVBox);
        AnchorPane.setTopAnchor(searchBarVBox, 48.0);
        AnchorPane.setRightAnchor(searchBarVBox, 8.0);
        searchField.requestFocus();
        searchField.selectAll();
    }

}
