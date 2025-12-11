package org.example.tonpad.ui.extentions;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.Setter;

public class SearchTreeItem extends TreeItem<String> {

    private final boolean isFile;

    @Setter
    @Getter
    private int firstMatchIndex;

    @Setter
    @Getter
    private String query;

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