package org.example.tonpad.ui.controllers.tree;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.tonpad.core.editor.enums.EditorMode;
import org.example.tonpad.core.exceptions.IllegalInputException;
import org.example.tonpad.core.exceptions.TonpadBaseException;
import org.example.tonpad.core.extentions.TriConsumer;
import org.example.tonpad.core.files.Buffer;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.core.sort.SortKey;
import org.example.tonpad.core.sort.SortOptions;
import org.example.tonpad.ui.controllers.AbstractController;
import org.example.tonpad.ui.controllers.action.SelectSnippetActionController;
import org.example.tonpad.ui.controllers.dialog.QuickStartDialogController;
import org.example.tonpad.ui.extentions.SnippetTreeItem;
import org.example.tonpad.ui.extentions.VaultPathsContainer;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnippetTreeController extends AbstractController {

    @FXML
    private TreeView<String> snippetTreeView;

    @FXML
    private VBox fileTreeVBox;

    @FXML
    private Button addSnippetButton;

    @FXML
    private Button addDirectoryButton;

    @FXML
    private Button refreshFilesButton;

    @FXML
    private Button sortSnippetsButton;

    @FXML
    private Button expandSnippetsButton;

    @FXML
    private Button changeVaultButton;

    @FXML
    private ContextMenu sortMenu;

    @FXML
    private ToggleGroup sortToggleGroup;

    @FXML
    private RadioMenuItem miNameAsc, miNameDesc, miCreatedNewest, miCreatedOldest;

    @FXML
    private CheckBox cbFoldersFirst, cbRelevantOnly;
    
    private SortKey sortKey = SortKey.NAME_ASC;

    private final QuickStartDialogController quickStartDialogController;

    private final FileSystemService fileSystemService;

    private final Buffer buffer;

    private final VaultPathsContainer vaultPathsContainer;

    private static final javafx.css.PseudoClass MATCHED = javafx.css.PseudoClass.getPseudoClass("matched");

    private TreeItem<String> rootItem;

    private TreeItem<String> selectedItem;

    private boolean isCollapsed = true;

    private final BooleanProperty programmaticEdit = new SimpleBooleanProperty(false);

    @Setter
    private TriConsumer<Path, Boolean, EditorMode> snippetOpenHandler;

    @Setter
    private BiConsumer<Path, Path> snippetRenameHandler;

    @Setter
    private Consumer<Path> snippetCloseHandler;

    @Setter
    private Consumer<Path> snippetInsertHandler;

    private final Map<String, Boolean> expandedState = new HashMap<>();

    private final SelectSnippetActionController selectSnippetActionController;

    ContextMenu actionMenu;

    public void init(AnchorPane parent) {
        parent.getChildren().add(fileTreeVBox);
        AnchorPane.setTopAnchor(fileTreeVBox, 0.0);
        AnchorPane.setBottomAnchor(fileTreeVBox, 0.0);
        AnchorPane.setLeftAnchor(fileTreeVBox, 0.0);
        AnchorPane.setRightAnchor(fileTreeVBox, 0.0);

        setupEventHandlers();
        setupFileTree();
    }

    private void setupEventHandlers() {
        changeVaultButton.setOnAction(e -> quickStartDialogController.show());

        expandSnippetsButton.setOnAction(e -> {
            ImageView imageView = (ImageView) expandSnippetsButton.getGraphic();

            if (isCollapsed) {
                expandAll();
                imageView.getStyleClass().remove("expand-icon");
                imageView.getStyleClass().add("collapse-icon");
            } else {
                collapseAll();
                imageView.getStyleClass().remove("collapse-icon");
                imageView.getStyleClass().add("expand-icon");
            }
            isCollapsed = !isCollapsed;
        });

        refreshFilesButton.setOnAction(e -> refreshTree());

        setupShortCuts();


        sortSnippetsButton.setOnAction(e -> {
            if (sortMenu == null) {
                throw new IllegalInputException("sortMenu is null");
            }
            if (sortMenu.isShowing()) sortMenu.hide();
            else sortMenu.show(sortSnippetsButton, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        cbFoldersFirst.selectedProperty().addListener((obs, o, n) -> applySortOptions());
        cbRelevantOnly.selectedProperty().addListener((obs, o, n) -> applySortOptions());
        miCreatedNewest.setUserData(SortKey.CREATED_NEWEST);
        miCreatedOldest.setUserData(SortKey.CREATED_OLDEST);
        miNameAsc.setUserData(SortKey.NAME_ASC);
        miNameDesc.setUserData(SortKey.NAME_DESC);

        if(sortToggleGroup.getSelectedToggle() != null)
        {
            Object ud = sortToggleGroup.getSelectedToggle().getUserData();
            if(ud instanceof SortKey sk) sortKey = sk;
        }

        miNameAsc.setOnAction(e -> onSortPicked(SortKey.NAME_ASC));
        miNameDesc.setOnAction(e -> onSortPicked(SortKey.NAME_DESC));
        miCreatedNewest.setOnAction(e -> onSortPicked(SortKey.CREATED_NEWEST));
        miCreatedOldest.setOnAction(e -> onSortPicked(SortKey.CREATED_OLDEST));
        
        addSnippetButton.setOnAction(e -> {
            onAddSnippet();
        });
        
        addDirectoryButton.setOnAction(e -> {
            onAddDirectory();
        });
        
        applySortOptions();
    }

    private void onSortPicked(SortKey picked) {
        if(sortKey != picked) sortKey = picked;
        applySortOptions();
        if(sortMenu != null && sortMenu.isShowing()) sortMenu.hide();
    }
    
    private void applySortOptions() {
        refreshTree();        
    }

    private void onOpenFile(TreeItem<String> target) {
        if (target != null && target.isLeaf()) {
            Path filePath = getFullPath(target);
            if (snippetOpenHandler != null) {
                snippetOpenHandler.accept(filePath, false, EditorMode.SNIPPET);
            }
        }
    }

    private void selectItem(Path path, boolean rename) {
        Path relativePath = vaultPathsContainer.getSnippetsPath().relativize(path);
        TreeItem<String> item = findTreeItemByPath(rootItem, relativePath);

        if (item != null) {
            if (rename) {
                onRename(item);
            } else {
                snippetTreeView.getSelectionModel().select(item);
                selectedItem = item;
                snippetTreeView.scrollTo(snippetTreeView.getRow(item));
            }
        }
    }

    private TreeItem<String> findTreeItemByPath(TreeItem<String> root, Path path) {
        if (path.getNameCount() == 0) return root;

        String firstSegment = path.getName(0).toString();
        for (TreeItem<String> child : root.getChildren()) {
            if (child.getValue().equals(firstSegment)) {
                if (path.getNameCount() == 1) {
                    return child;
                } else {
                    return findTreeItemByPath(child, path.subpath(1, path.getNameCount()));
                }
            }
        }
        return null;
    }

    public void refreshTree() {
        saveAllExpandedStates();
        SortOptions opt = new SortOptions(sortKey, cbFoldersFirst.isSelected(), cbRelevantOnly.isSelected());
        FileTree fileTree = fileSystemService.getFileTreeSorted(vaultPathsContainer.getSnippetsPath(), opt);
        TreeItem<String> newRoot = convertFileTreeToTreeItem(fileTree);

        snippetTreeView.setRoot(newRoot);
        snippetTreeView.setShowRoot(false);

        rootItem = newRoot;

        restoreAllExpandedStates();
    }

    private void saveAllExpandedStates() {
        expandedState.clear();
        if (rootItem != null) {
            saveExpandedStateRecursive(rootItem);
        }
    }

    private void saveExpandedStateRecursive(TreeItem<String> item) {
        if (item != null) {
            String path = getRelativePath(item);
            expandedState.put(path, item.isExpanded());

            for (TreeItem<String> child : item.getChildren()) {
                saveExpandedStateRecursive(child);
            }
        }
    }

    private void restoreAllExpandedStates() {
        if (rootItem != null) {
            restoreExpandedStateRecursive(rootItem);
        }
    }

    private void restoreExpandedStateRecursive(TreeItem<String> item) {
        if (item != null) {
            String path = getRelativePath(item);
            Boolean wasExpanded = expandedState.get(path);
            if (wasExpanded != null) {
                item.setExpanded(wasExpanded);
            }

            for (TreeItem<String> child : item.getChildren()) {
                restoreExpandedStateRecursive(child);
            }
        }
    }

    private String getRelativePath(TreeItem<String> item) {
        var parts = new ArrayList<String>();
        for (TreeItem<String> cur = item; cur != null; cur = cur.getParent()) {
            if (cur.getValue() == null || cur.getValue().isEmpty()) break;
            parts.addFirst(cur.getValue());
        }
        return String.join("/", parts).replace('\\','/');
    }

    private String getVaultRelativePath(TreeItem<String> item) {
        var parts = new ArrayList<String>();
        for (TreeItem<String> cur = item; cur != null && cur.getParent() != null; cur = cur.getParent()) {
            if (cur.getValue() == null || cur.getValue().isEmpty()) break;
            parts.addFirst(cur.getValue());
        }
        return String.join("/", parts).replace('\\','/');
    }

    private Path getFullPath(TreeItem<String> item) {
        if (item == null) {
            return vaultPathsContainer.getSnippetsPath();
        }

        List<String> pathSegments = new ArrayList<>();
        TreeItem<String> current = item;

        while (current.getParent() != null) {
            if (current instanceof SnippetTreeItem snippetItem) {
                pathSegments.addFirst(snippetItem.getFullName());
            } else {
                pathSegments.addFirst(current.getValue());
            }
            current = current.getParent();
        }

        Path result = vaultPathsContainer.getSnippetsPath();
        for (String segment : pathSegments) {
            result = result.resolve(segment);
        }
        return result;
    }

    private void expandAll() {
        expandAllRecursive(rootItem);
    }

    private void collapseAll() {
        collapseAllRecursive(rootItem);
    }

    private void expandAllRecursive(TreeItem<String> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<String> child : item.getChildren()) {
                expandAllRecursive(child);
            }
        }
    }

    private void collapseAllRecursive(TreeItem<String> item) {
        if (item != null) {
            item.setExpanded(false);
            for (TreeItem<String> child : item.getChildren()) {
                collapseAllRecursive(child);
            }
        }
    }

    private void setupFileTree() {
        FileTree fileTree = fileSystemService.getFileTree(vaultPathsContainer.getSnippetsPath());

        rootItem = convertFileTreeToTreeItem(fileTree);

        snippetTreeView.setRoot(rootItem);
        snippetTreeView.setShowRoot(false);

        buildContextMenu();
        snippetTreeView.setContextMenu(actionMenu);

        rootItem.setExpanded(true);

        snippetTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onFileSelected(newValue)
        );


        snippetTreeView.setEditable(true);
        snippetTreeView.setCellFactory(tv -> new EditableFileCell());

        snippetTreeView.setOnEditStart(ev -> {
            if (!programmaticEdit.get()) {
                ev.consume();
                Platform.runLater(() -> snippetTreeView.edit(null));
            } else {
                programmaticEdit.set(false);
            }
        });
        snippetTreeView.setOnEditCommit(ev -> programmaticEdit.set(false));
        snippetTreeView.setOnEditCancel(ev -> programmaticEdit.set(false));

        snippetTreeView.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                var ti = snippetTreeView.getSelectionModel().getSelectedItem();
                onOpenFile(ti);
                e.consume();
            } else if ((e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.SECONDARY) &&
                    e.getClickCount() == 1) {
                if (e.getTarget() == snippetTreeView ||
                        (e.getTarget() instanceof TreeCell && ((TreeCell<?>) e.getTarget()).getItem() == null)) {
                    snippetTreeView.getSelectionModel().clearSelection();
                }
            }
        });

        actionMenu.setOnShowing(e -> {
            if (selectedItem != null) {
                updateMenuItemsState();
            } else {
                updateEmptyAreaMenuItemsState();
            }
        });
    }

    private void updateEmptyAreaMenuItemsState() {
        boolean hasClipboard = buffer != null && buffer.getCopyBuffer() != null && !buffer.getCopyBuffer().isEmpty();

        selectSnippetActionController.getOpenInCurrentTabMenuItem().setDisable(true);
        selectSnippetActionController.getPasteMenuItem().setDisable(!hasClipboard);

        selectSnippetActionController.getCopyMenuItem().setDisable(true);
        selectSnippetActionController.getCutMenuItem().setDisable(true);
        selectSnippetActionController.getCopyAbsolutePathMenuItem().setDisable(true);
        selectSnippetActionController.getCopyRelativePathMenuItem().setDisable(true);
        selectSnippetActionController.getShowInNotepadMenuItem().setDisable(true);
        selectSnippetActionController.getRenameMenuItem().setDisable(true);
        selectSnippetActionController.getRemoveMenuItem().setDisable(true);

        selectSnippetActionController.getInsertSnippetMenuItem().setDisable(true);
        selectSnippetActionController.getNewSnippetMenuItem().setDisable(false);
        selectSnippetActionController.getNewFolderMenuItem().setDisable(false);
        selectSnippetActionController.getCopyVaultPathMenuItem().setDisable(false);
    }

    private boolean canPasteInto(TreeItem<String> ti) {
        if (ti == null) return false;
        Path p = getFullPath(ti);
        if (!ti.isLeaf()) return Files.isDirectory(p);
        var parent = p.getParent();
        return parent != null && Files.isDirectory(parent);
    }

    private void buildContextMenu() {
        actionMenu = selectSnippetActionController.createContextMenu();
        setupMenuButtonHandlers();
    }

    private void setupMenuButtonHandlers() {
        selectSnippetActionController.getOpenInCurrentTabMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onOpenInNewTab(selectedItem);
        });

        selectSnippetActionController.getNewSnippetMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onAddSnippet();
        });

        selectSnippetActionController.getNewFolderMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onAddDirectory();
        });

        selectSnippetActionController.getInsertSnippetMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onInsertSnippet(selectedItem);
        });

        selectSnippetActionController.getCopyMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopy(selectedItem);
        });

        selectSnippetActionController.getCutMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCut(selectedItem);
        });

        selectSnippetActionController.getPasteMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onPaste(selectedItem);
        });

        selectSnippetActionController.getCopyVaultPathMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopyVaultPath();
        });

        selectSnippetActionController.getCopyAbsolutePathMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopyAbsPath(selectedItem);
        });

        selectSnippetActionController.getCopyRelativePathMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopyRelPath(selectedItem);
        });

        selectSnippetActionController.getShowInNotepadMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onShowInNotepad(selectedItem);
        });

        selectSnippetActionController.getShowInExplorerMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onShowInExplorer(selectedItem);
        });

        selectSnippetActionController.getRenameMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onRename(selectedItem);
        });

        selectSnippetActionController.getRemoveMenuItem().setOnAction(e -> {
            actionMenu.hide();
            confirmAndDeleteForItem(selectedItem);
        });
    }

    private void updateMenuItemsState() {
        TreeItem<String> treeItem = selectedItem;
        boolean canPasteHere = canPasteInto(treeItem);
        boolean hasClipboard = buffer != null && buffer.getCopyBuffer() != null && !buffer.getCopyBuffer().isEmpty();

        selectSnippetActionController.getPasteMenuItem().setDisable(!(canPasteHere && hasClipboard));

        boolean isRoot = treeItem != null && treeItem.getParent() == null;
        selectSnippetActionController.getRenameMenuItem().setDisable(isRoot);
        selectSnippetActionController.getRemoveMenuItem().setDisable(isRoot);

        boolean isDirectory = !treeItem.isLeaf();
        selectSnippetActionController.getNewSnippetMenuItem().setDisable(!isDirectory);
        selectSnippetActionController.getNewFolderMenuItem().setDisable(!isDirectory);

        boolean isMdFile = fileSystemService.isMarkdownFile(treeItem.getValue());
        selectSnippetActionController.getOpenInCurrentTabMenuItem().setDisable(!isMdFile);
        selectSnippetActionController.getShowInNotepadMenuItem().setDisable(!isMdFile);

        selectSnippetActionController.getCopyVaultPathMenuItem().setDisable(true);
        selectSnippetActionController.getCopyAbsolutePathMenuItem().setDisable(false);
        selectSnippetActionController.getCopyRelativePathMenuItem().setDisable(false);

        selectSnippetActionController.getInsertSnippetMenuItem().setDisable(false);
        selectSnippetActionController.getNewSnippetMenuItem().setDisable(true);
        selectSnippetActionController.getNewFolderMenuItem().setDisable(true);
        selectSnippetActionController.getCopyMenuItem().setDisable(false);
        selectSnippetActionController.getCutMenuItem().setDisable(false);
    }

    private void onOpenInNewTab(TreeItem<String> target) {
        if (target != null && target.isLeaf()) {
            Path filePath = getFullPath(target);
            if (snippetOpenHandler != null) {
                snippetOpenHandler.accept(filePath, true, EditorMode.SNIPPET);
            }
        }
    }

    private void onAddSnippet() {
        Path newFilePath = addSnippet();
        refreshTree();
        selectItem(newFilePath, true);
        snippetOpenHandler.accept(newFilePath, false, EditorMode.SNIPPET);
    }

    private void onAddDirectory() {
        Path newDirPath = addDir();
        refreshTree();
        selectItem(newDirPath, true);
    }

    private void onInsertSnippet(TreeItem<String> node) {
        if (node != null) {
            snippetInsertHandler.accept(getFullPath(node));
        }
    }

    private void onCopy(TreeItem<String> node) {
        if (node != null) {
            fileSystemService.copyFile(getFullPath(node));
        }
    }

    private void onCut(TreeItem<String> node) {
        if (node != null) {
            fileSystemService.cutFile(getFullPath(node));
        }
    }

    private void onPaste(TreeItem<String> target) {
        Path targetPath;

        if (target != null) {
            if (Files.isDirectory(getFullPath(target))) {
                targetPath = getFullPath(target);
            } else {
                targetPath = getFullPath(target.getParent());
            }
        } else {
            targetPath = vaultPathsContainer.getSnippetsPath();
        }

        fileSystemService.pasteFile(targetPath);
        refreshTree();
    }

    private void onCopyVaultPath() {
        var cc = new ClipboardContent();
        cc.putString(vaultPathsContainer.getSnippetsPath().toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private void onCopyAbsPath(TreeItem<String> node) {
        if (node != null) {
            var cc = new ClipboardContent();
            cc.putString(getFullPath(node).toString());
            Clipboard.getSystemClipboard().setContent(cc);
        }
    }

    private void onCopyRelPath(TreeItem<String> node) {
        if (node != null) {
            var cc = new ClipboardContent();
            cc.putString(getVaultRelativePath(node));
            Clipboard.getSystemClipboard().setContent(cc);
        }
    }

    private void onShowInNotepad(TreeItem<String> node) {
        if (node != null) {
            fileSystemService.showFileInNotepad(getFullPath(node));
        }
    }

    private void onShowInExplorer(TreeItem<String> node) {
        if (node != null) {
            fileSystemService.showFileInExplorer(getFullPath(node));
        } else {
            fileSystemService.showFileInExplorer(vaultPathsContainer.getSnippetsPath());
        }
    }

    private void onRename(TreeItem<String> node) {
        if (node != null && node.getParent() != null) {
            snippetTreeView.getSelectionModel().select(node);
            snippetTreeView.getFocusModel().focus(snippetTreeView.getRow(node));
            snippetTreeView.scrollTo(snippetTreeView.getRow(node));
            snippetTreeView.requestFocus();

            programmaticEdit.set(true);
            Platform.runLater(() -> snippetTreeView.edit(node));
        }
    }

    private final class EditableFileCell extends TreeCell<String> {

        private TextField editor;
        private final ImageView iconView = new ImageView();

        public EditableFileCell() {
            setEditable(true);
            iconView.setFitWidth(16);
            iconView.setFitHeight(16);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setEditable(true);

            if(empty || item == null) {
                pseudoClassStateChanged(MATCHED, false);
                setText(null);
                setGraphic(null);
                setStyle("");
                getStyleClass().remove("matched");
                return;
            }

            determineIcon(getTreeItem());

            if (isEditing()) {
                if (editor == null) editor = createEditor();
                if (getTreeItem() instanceof SnippetTreeItem snippetItem) {
                    editor.setText(snippetItem.getDisplayName());
                } else {
                    editor.setText(item);
                }
                setText(null);
                setGraphic(editor);
            } else {
                setText(item);
                setGraphic(iconView);
                pseudoClassStateChanged(MATCHED, false);
            }
        }

        private void determineIcon(TreeItem<String> treeItem) {
            if (treeItem == null) return;

            iconView.getStyleClass().removeAll("snippet-colored-icon", "folder-colored-icon");

            if (!treeItem.isLeaf()) {
                iconView.getStyleClass().add("folder-colored-icon");
            } else {
                iconView.getStyleClass().add("snippet-colored-icon");
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable() || getTreeView()==null || !getTreeView().isEditable()) return;
            if (getItem() == null) return;
            super.startEdit();

            if (editor == null) editor = createEditor();
            editor.setText(getItem());
            setText(null);
            setGraphic(editor);

            editor.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            determineIcon(getTreeItem());
            setGraphic(iconView);
        }

        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            determineIcon(getTreeItem());
            setGraphic(iconView);
            setText(newValue);
        }

        private TextField createEditor() {
            var tf = new TextField(getItem());

            tf.setOnAction(ev -> finishRename(tf.getText()));

            tf.setOnKeyPressed(ke -> {
                if (ke.getCode() == KeyCode.ESCAPE) cancelEdit();
            });

            tf.focusedProperty().addListener((obs, was, now) -> {
                if (!now) {
                    cancelEdit();
                }
            });

            tf.focusedProperty().addListener((obs, was, now) -> {
                if (now) {
                    Platform.runLater(() -> {
                        String cur = getItem() == null ? "" : getItem();
                        if (Files.isDirectory(getFullPath(getTreeItem()))) {
                            tf.selectAll();
                        } else {
                            var parts = splitName(cur);
                            tf.selectRange(0, parts.base.length());
                        }
                    });
                }
            });
            return tf;
        }

        private void finishRename(String newDisplayName) {
            String oldDisplayName = getItem();
            if (newDisplayName == null) newDisplayName = "";
            newDisplayName = newDisplayName.trim();
            if (newDisplayName.isEmpty() || newDisplayName.equals(oldDisplayName)) {
                cancelEdit();
                return;
            }

            var ti = getTreeItem();
            if (ti == null || ti.getParent() == null) {
                cancelEdit();
                return;
            }

            tryRename(ti, newDisplayName);
            commitEdit(newDisplayName);
        }

        private NameParts splitName(String fileName) {
            if (getTreeItem() instanceof SnippetTreeItem snippetItem && snippetItem.isLeaf()) {
                return new NameParts(snippetItem.getDisplayName(), "");
            }

            int dot = fileName.lastIndexOf('.');
            if (dot > 0 && dot < fileName.length() - 1) {
                return new NameParts(fileName.substring(0, dot), fileName.substring(dot));
            }
            return new NameParts(fileName, "");
        }
    }

    private record NameParts(String base, String ext) {
    }

    private void tryRename(TreeItem<String> ti, String newDisplayName) {
        if (!(ti instanceof SnippetTreeItem snippetItem)) {
            throw new TonpadBaseException("Invalid tree item type");
        }

        Path oldAbs = getFullPath(ti);
        Path parent = oldAbs.getParent();
        if (parent == null) {
            throw new TonpadBaseException("Cannot rename, because parent is null");
        }

        Path newAbs = getAbs(newDisplayName, snippetItem, parent);

        fileSystemService.rename(oldAbs.toString(), newAbs.toString());

        refreshTree();
        selectItem(newAbs, false);
        snippetRenameHandler.accept(oldAbs, newAbs);
    }

    private static Path getAbs(String newDisplayName, SnippetTreeItem snippetItem, Path parent) {
        String newFullName;
        if (!snippetItem.isLeaf()) {
            newFullName = newDisplayName;
        } else {
            String oldFullName = snippetItem.getFullName();
            int dotIndex = oldFullName.lastIndexOf('.');
            if (dotIndex > 0) {
                String extension = oldFullName.substring(dotIndex);
                newFullName = newDisplayName + extension;
            } else {
                newFullName = newDisplayName + ".md";
            }
        }

        return parent.resolve(newFullName);
    }

    private void onFileSelected(TreeItem<String> selectedItem) {
        this.selectedItem = selectedItem;
    }

    private TreeItem<String> convertFileTreeToTreeItem(FileTree fileTree) {
        Path path = fileTree.getPath();
        String fileName = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        boolean isDirectory = fileTree.getChildren() != null;

        if (!isDirectory && !fileSystemService.isMarkdownFile(path))
            return null;

        String displayName = fileName;

        if (!isDirectory) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                displayName = fileName.substring(0, dotIndex);
            }
        }

        SnippetTreeItem treeItem = new SnippetTreeItem(displayName, fileName, isDirectory);

        if (isDirectory && !fileTree.getChildren().isEmpty()) {
            for (FileTree child : fileTree.getChildren()) {
                TreeItem<String> childItem = convertFileTreeToTreeItem(child);
                if (childItem != null) {
                    snippetTreeView.setEditable(true);
                    treeItem.getChildren().add(childItem);
                }
            }
        }

        return treeItem;
    }

    private Path addSnippet() {
        Path targetPath;

        if (selectedItem == null) {
            targetPath = vaultPathsContainer.getSnippetsPath();
        } else {
            Path selectedPath = getFullPath(selectedItem);

            if (selectedItem.isLeaf()) {
                targetPath = selectedPath.getParent();
            } else {
                targetPath = selectedPath;
            }
        }

        Path filePath = targetPath.resolve("Untitled.md");

        int index = 1;
        while (fileSystemService.exists(filePath)) {
            filePath = targetPath.resolve("Untitled " + index + ".md");
            index += 1;
        }

        fileSystemService.makeFile(filePath);
        return filePath;
    }

    private Path addDir() {
        Path targetPath;

        if (selectedItem == null) {
            targetPath = vaultPathsContainer.getSnippetsPath();
        } else {
            Path selectedPath = getFullPath(selectedItem);

            if (selectedItem.isLeaf()) {
                targetPath = selectedPath.getParent();
            } else {
                targetPath = selectedPath;
            }
        }

        Path dirPath = targetPath.resolve("Untitled");

        int index = 1;
        while (fileSystemService.exists(dirPath)) {
            dirPath = targetPath.resolve("Untitled " + index);
            index += 1;
        }

        fileSystemService.makeDir(dirPath);

        return dirPath;
    }

    private TreeItem<String> getSelected() {
        return snippetTreeView.getSelectionModel().getSelectedItem();
    }

    private void setupShortCuts() {
        fileTreeVBox.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                attachAccelerator(newS, new KeyCodeCombination(KeyCode.DELETE, KeyCodeCombination.CONTROL_DOWN), this::deleteSelected);

                attachAccelerator(newS, new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN, KeyCodeCombination.ALT_DOWN), () -> onCopy(getSelected()));

                attachAccelerator(newS, new KeyCodeCombination(KeyCode.V, KeyCodeCombination.CONTROL_DOWN, KeyCodeCombination.ALT_DOWN), () -> onPaste(getSelected()));

                attachAccelerator(newS, new KeyCodeCombination(KeyCode.X, KeyCodeCombination.CONTROL_DOWN, KeyCodeCombination.ALT_DOWN), () -> onCut(getSelected()));

            }
        });
        snippetTreeView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F2) {
                e.consume();
                onRename(getSelected());
            }
        });
    }

    private void confirmAndDeleteForItem(TreeItem<String> node) {
        if (node == null)
            return;
        if (node.getParent() == null)
            return;

        var owner = fileTreeVBox.getScene() != null ? fileTreeVBox.getScene().getWindow() : null;

        var alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete selected file or folder?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        if (owner != null) alert.initOwner(owner);

        var res = alert.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        Path fullPath = getFullPath(node);

        fileSystemService.delete(fullPath);
        refreshTree();
        snippetCloseHandler.accept(fullPath);
    }

    private void deleteSelected() {
        var sel = snippetTreeView.getSelectionModel().getSelectedItem();
        confirmAndDeleteForItem(sel);
    }

    private void attachAccelerator(Scene scene, KeyCodeCombination keyComb, Runnable callback) {
        scene.getAccelerators().put(
                keyComb,
                () -> Platform.runLater(callback)
        );
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/tree/snippet-tree-panel.fxml";
    }

}