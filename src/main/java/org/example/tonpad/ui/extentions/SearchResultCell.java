package org.example.tonpad.ui.extentions;

import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class SearchResultCell extends TreeCell<String> {

    private final String currentQuery;
    private final ImageView iconView = new ImageView();

    public SearchResultCell(String currentQuery) {
        this.currentQuery = currentQuery;
        iconView.setFitWidth(16);
        iconView.setFitHeight(16);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        iconView.getStyleClass().remove("note-colored-icon");

        List<TextRange> matches = findAllMatchesIncludingOverlapping(item, currentQuery);

        boolean showIcon = !getTreeItem().isLeaf();
        if (showIcon) {
            iconView.getStyleClass().add("note-colored-icon");
        }

        if (matches.isEmpty()) {
            if (showIcon) {
                HBox container = new HBox(5);
                container.getChildren().addAll(iconView, new Text(item));
                setGraphic(container);
                setText(null);
            } else {
                setGraphic(null);
                setText(item);
            }
        } else {
            setText(null);
            if (showIcon) {
                HBox container = new HBox(5);
                container.getChildren().addAll(iconView, buildHighlightedName(item, matches));
                setGraphic(container);
            } else {
                setGraphic(buildHighlightedName(item, matches));
            }
        }
    }

    private List<TextRange> findAllMatchesIncludingOverlapping(String text, String query) {
        List<TextRange> matches = new ArrayList<>();
        if (query == null || query.isEmpty()) return matches;

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int index = 0;
        while ((index = lowerText.indexOf(lowerQuery, index)) != -1) {
            matches.add(new TextRange(index, index + query.length()));
            index += 1; // Ключевое изменение: сдвигаем на 1 символ для пересекающихся
        }
        return matches;
    }

    private TextFlow buildHighlightedName(String text, List<TextRange> matches) {
        TextFlow textFlow = new TextFlow();
        int lastIndex = 0;

        for (TextRange range : matches) {
            if (range.start > lastIndex) {
                Text before = new Text(text.substring(lastIndex, range.start));
                textFlow.getChildren().add(before);
            }

            Text highlight = new Text(text.substring(range.start, range.end));
            highlight.getStyleClass().add("file-tree-hit");
            textFlow.getChildren().add(highlight);

            lastIndex = range.end;
        }

        if (lastIndex < text.length()) {
            Text after = new Text(text.substring(lastIndex));
            textFlow.getChildren().add(after);
        }

        return textFlow;
    }

    private record TextRange(int start, int end) {
    }
}