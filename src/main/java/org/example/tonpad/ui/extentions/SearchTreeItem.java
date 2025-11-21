package org.example.tonpad.ui.extentions;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class SearchTreeItem extends TreeItem<String> {

    private final boolean isFile;

    public SearchTreeItem(String name, boolean isFile) {
        super(name);
        this.isFile = isFile;
    }

    @Override
    public boolean isLeaf() {
        return !isFile;
    }

    @Override
    public ObservableList<TreeItem<String>> getChildren() {
        return super.getChildren();
    }
}