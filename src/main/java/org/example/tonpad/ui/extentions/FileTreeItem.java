package org.example.tonpad.ui.extentions;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;


public class FileTreeItem extends TreeItem<String> {
    private final boolean isDirectory;

    public FileTreeItem(String name, boolean isDirectory) {
        super(name);
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