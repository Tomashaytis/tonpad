package org.example.tonpad.ui.extentions;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.TreeCell;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class SearchResultCell extends TreeCell<String> {

    private final String currentQuery;

    public SearchResultCell(String currentQuery) {
        this.currentQuery = currentQuery;
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        List<TextRange> matches = findMatches(item, currentQuery);
        if (matches.isEmpty()) {
            setGraphic(null);
            setText(item);
        } else {
            setText(null);
            setGraphic(buildHighlightedName(item, matches));
        }
    }

    private List<TextRange> findMatches(String text, String query) {
        List<TextRange> matches = new ArrayList<>();
        if (query == null || query.isEmpty()) return matches;

        String lowerText = text.toLowerCase();
        int index = 0;
        while ((index = lowerText.indexOf(query, index)) != -1) {
            matches.add(new TextRange(index, index + query.length()));
            index += query.length();
        }
        return matches;
    }

    private TextFlow buildHighlightedName(String text, List<TextRange> matches) {
        TextFlow textFlow = new TextFlow();
        int lastIndex = 0;

        for (TextRange range : matches) {
            // Текст до совпадения
            if (range.start > lastIndex) {
                Text before = new Text(text.substring(lastIndex, range.start));
                textFlow.getChildren().add(before);
            }

            Text highlight = new Text(text.substring(range.start, range.end));
            highlight.getStyleClass().add("filetree-hit");
            textFlow.getChildren().add(highlight);

            lastIndex = range.end;
        }

        if (lastIndex < text.length()) {
            Text after = new Text(text.substring(lastIndex));
            textFlow.getChildren().add(after);
        }

        return textFlow;
    }

    private static class TextRange {
        final int start;
        final int end;
        TextRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}