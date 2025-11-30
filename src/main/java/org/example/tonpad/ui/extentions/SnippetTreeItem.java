package org.example.tonpad.ui.extentions;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import lombok.Getter;

public class SnippetTreeItem extends TreeItem<String> {

    private final boolean isDirectory;

    @Getter
    private final String fullName;

    @Getter
    private final String displayName;

    public SnippetTreeItem(String displayName, String fullName, boolean isDirectory) {
        super(displayName);
        this.displayName = displayName;
        this.fullName = fullName;
        this.isDirectory = isDirectory;
    }

    @Override
    public boolean isLeaf() {
        return !isDirectory;
    }

    @Override
    public ObservableList<TreeItem<String>> getChildren() {
        return super.getChildren();
    }
}