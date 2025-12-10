package org.example.tonpad.ui.controllers.tree;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.example.tonpad.core.editor.enums.EditorMode;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.exceptions.IllegalInputException;
import org.example.tonpad.core.exceptions.TonpadBaseException;
import org.example.tonpad.core.extentions.TriConsumer;
import org.example.tonpad.core.files.Buffer;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.EncryptorFactory;
import org.example.tonpad.core.sort.SortKey;
import org.example.tonpad.core.sort.SortOptions;
import org.example.tonpad.ui.controllers.AbstractController;
import org.example.tonpad.ui.controllers.dialog.QuickStartDialogController;
import org.example.tonpad.ui.controllers.action.SelectFileActionController;
import org.example.tonpad.ui.extentions.FileTreeItem;
import org.example.tonpad.ui.extentions.VaultPathsContainer;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileTreeController extends AbstractController {

    @FXML
    private TreeView<String> fileTreeView;

    @FXML
    private VBox fileTreeVBox;

    @FXML
    private Button addNoteButton;

    @FXML
    private Button addDirectoryButton;

    @FXML
    private Button refreshFilesButton;

    @FXML
    private Button sortFilesButton;

    @FXML
    private Button expandFilesButton;

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
    private TriConsumer<Path, Boolean, EditorMode> noteOpenHandler;

    @Setter
    private BiConsumer<Path, Path> noteRenameHandler;

    @Setter
    private Consumer<Path> noteCloseHandler;

    @Setter
    private Map<String, List<SearchService.Hit>> hitsMap = Collections.emptyMap();

    private final Map<String, Boolean> expandedState = new HashMap<>();

    private final SelectFileActionController selectFileActionController;

    private final EncryptorFactory encryptorFactory;

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

        expandFilesButton.setOnAction(e -> {
            ImageView imageView = (ImageView) expandFilesButton.getGraphic();

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


        sortFilesButton.setOnAction(e -> {
            if (sortMenu == null) {
                throw new IllegalInputException("sortMenu is null");
            }
            if (sortMenu.isShowing()) sortMenu.hide();
            else sortMenu.show(sortFilesButton, javafx.geometry.Side.BOTTOM, 0, 0);
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
        
        addNoteButton.setOnAction(e -> {
            onAddNote();
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
            if (noteOpenHandler != null) {
                noteOpenHandler.accept(filePath, false, EditorMode.NOTE);
            }
        }
    }

    private void selectItem(Path path, boolean rename) {
        Path relativePath = vaultPathsContainer.getNotesPath().relativize(path);
        TreeItem<String> item = findTreeItemByPath(rootItem, relativePath);

        if (item != null) {
            if (rename) {
                onRename(item);
            } else {
                fileTreeView.getSelectionModel().select(item);
                selectedItem = item;
                fileTreeView.scrollTo(fileTreeView.getRow(item));
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
        FileTree fileTree = fileSystemService.getFileTreeSorted(vaultPathsContainer.getNotesPath(), opt);
        TreeItem<String> newRoot = convertFileTreeToTreeItem(fileTree);

        fileTreeView.setRoot(newRoot);
        fileTreeView.setShowRoot(false);

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
            return vaultPathsContainer.getNotesPath();
        }

        List<String> pathSegments = new ArrayList<>();
        TreeItem<String> current = item;

        while (current.getParent() != null) {
            pathSegments.addFirst(current.getValue());
            current = current.getParent();
        }

        Path result = vaultPathsContainer.getNotesPath();
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
        FileTree fileTree = fileSystemService.getFileTree(vaultPathsContainer.getNotesPath());

        rootItem = convertFileTreeToTreeItem(fileTree);

        fileTreeView.setRoot(rootItem);
        fileTreeView.setShowRoot(false);

        buildContextMenu();
        fileTreeView.setContextMenu(actionMenu);

        rootItem.setExpanded(true);

        fileTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onFileSelected(newValue)
        );


        fileTreeView.setEditable(true);
        fileTreeView.setCellFactory(tv -> new EditableFileCell());

        fileTreeView.setOnEditStart(ev -> {
            if (!programmaticEdit.get()) {
                ev.consume();
                Platform.runLater(() -> fileTreeView.edit(null));
            } else {
                programmaticEdit.set(false);
            }
        });
        fileTreeView.setOnEditCommit(ev -> programmaticEdit.set(false));
        fileTreeView.setOnEditCancel(ev -> programmaticEdit.set(false));

        fileTreeView.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                var ti = fileTreeView.getSelectionModel().getSelectedItem();
                onOpenFile(ti);
                e.consume();
            } else if ((e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.SECONDARY) &&
                    e.getClickCount() == 1) {
                if (e.getTarget() == fileTreeView ||
                        (e.getTarget() instanceof TreeCell && ((TreeCell<?>) e.getTarget()).getItem() == null)) {
                    fileTreeView.getSelectionModel().clearSelection();
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

        selectFileActionController.getOpenInCurrentTabMenuItem().setDisable(true);
        selectFileActionController.getPasteMenuItem().setDisable(!hasClipboard);

        selectFileActionController.getCopyMenuItem().setDisable(true);
        selectFileActionController.getCutMenuItem().setDisable(true);
        selectFileActionController.getCopyAbsolutePathMenuItem().setDisable(true);
        selectFileActionController.getCopyRelativePathMenuItem().setDisable(true);
        selectFileActionController.getShowInNotepadMenuItem().setDisable(true);
        selectFileActionController.getRenameMenuItem().setDisable(true);
        selectFileActionController.getRemoveMenuItem().setDisable(true);

        selectFileActionController.getNewNoteMenuItem().setDisable(false);
        selectFileActionController.getNewFolderMenuItem().setDisable(false);
        selectFileActionController.getCopyVaultPathMenuItem().setDisable(false);
    }

    private boolean canPasteInto(TreeItem<String> ti) {
        if (ti == null) return false;
        Path p = getFullPath(ti);
        if (!ti.isLeaf()) return Files.isDirectory(p);
        var parent = p.getParent();
        return parent != null && Files.isDirectory(parent);
    }

    private void buildContextMenu() {
        actionMenu = selectFileActionController.createContextMenu();
        setupMenuButtonHandlers();
    }

    private void setupMenuButtonHandlers() {
        selectFileActionController.getOpenInCurrentTabMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onOpenInNewTab(selectedItem);
        });

        selectFileActionController.getNewNoteMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onAddNote();
        });

        selectFileActionController.getNewFolderMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onAddDirectory();
        });

        selectFileActionController.getCopyMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopy(selectedItem);
        });

        selectFileActionController.getCutMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCut(selectedItem);
        });

        selectFileActionController.getPasteMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onPaste(selectedItem);
        });

        selectFileActionController.getCopyVaultPathMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopyVaultPath();
        });

        selectFileActionController.getCopyAbsolutePathMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopyAbsPath(selectedItem);
        });

        selectFileActionController.getCopyRelativePathMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onCopyRelPath(selectedItem);
        });

        selectFileActionController.getShowInNotepadMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onShowInNotepad(selectedItem);
        });

        selectFileActionController.getShowInExplorerMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onShowInExplorer(selectedItem);
        });

        selectFileActionController.getRenameMenuItem().setOnAction(e -> {
            actionMenu.hide();
            onRename(selectedItem);
        });

        selectFileActionController.getRemoveMenuItem().setOnAction(e -> {
            actionMenu.hide();
            confirmAndDeleteForItem(selectedItem);
        });
    }

    private void updateMenuItemsState() {
        TreeItem<String> treeItem = selectedItem;
        boolean canPasteHere = canPasteInto(treeItem);
        boolean hasClipboard = buffer != null && buffer.getCopyBuffer() != null && !buffer.getCopyBuffer().isEmpty();

        selectFileActionController.getPasteMenuItem().setDisable(!(canPasteHere && hasClipboard));

        boolean isRoot = treeItem != null && treeItem.getParent() == null;
        selectFileActionController.getRenameMenuItem().setDisable(isRoot);
        selectFileActionController.getRemoveMenuItem().setDisable(isRoot);

        boolean isDirectory = !treeItem.isLeaf();
        selectFileActionController.getNewNoteMenuItem().setDisable(!isDirectory);
        selectFileActionController.getNewFolderMenuItem().setDisable(!isDirectory);

        boolean isMdFile = fileSystemService.isMarkdownFile(treeItem.getValue());
        selectFileActionController.getOpenInCurrentTabMenuItem().setDisable(!isMdFile);
        selectFileActionController.getShowInNotepadMenuItem().setDisable(!isMdFile);

        selectFileActionController.getCopyVaultPathMenuItem().setDisable(true);
        selectFileActionController.getCopyAbsolutePathMenuItem().setDisable(false);
        selectFileActionController.getCopyRelativePathMenuItem().setDisable(false);

        selectFileActionController.getCopyMenuItem().setDisable(false);
        selectFileActionController.getCutMenuItem().setDisable(false);
    }

    private void onOpenInNewTab(TreeItem<String> target) {
        if (target != null && target.isLeaf()) {
            Path filePath = getFullPath(target);
            if (noteOpenHandler != null) {
                noteOpenHandler.accept(filePath, true, EditorMode.NOTE);
            }
        }
    }

    private void onAddNote() {
        Path newFilePath = addNote();
        refreshTree();
        selectItem(newFilePath, true);
        noteOpenHandler.accept(newFilePath, false, EditorMode.NOTE);
    }

    private void onAddDirectory() {
        Path newDirPath = addDir();
        refreshTree();
        selectItem(newDirPath, true);
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
            targetPath = vaultPathsContainer.getNotesPath();
        }

        fileSystemService.pasteFile(targetPath);
        refreshTree();
    }

    private void onCopyVaultPath() {
        var cc = new ClipboardContent();
        cc.putString(vaultPathsContainer.getNotesPath().toString());
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
            fileSystemService.showFileInExplorer(vaultPathsContainer.getNotesPath());
        }
    }

    private void onRename(TreeItem<String> node) {
        if (node != null && node.getParent() != null) {
            fileTreeView.getSelectionModel().select(node);
            fileTreeView.getFocusModel().focus(fileTreeView.getRow(node));
            fileTreeView.scrollTo(fileTreeView.getRow(node));
            fileTreeView.requestFocus();

            programmaticEdit.set(true);
            Platform.runLater(() -> fileTreeView.edit(node));
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

            if(empty || item == null)
            {
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
                editor.setText(item);
                setText(null);
                setGraphic(editor);
            } else {
                String rel = norm(getRelativePath(getTreeItem()));
                var ranges = (hitsMap != null) ? hitsMap.get(rel) : null;
                if (ranges == null || ranges.isEmpty()) {
                    setText(item);
                    setGraphic(iconView);
                    pseudoClassStateChanged(MATCHED, false);
                } else {
                    setText(null);
                    var highlightedText = buildHighlightedName(item, ranges);
                    HBox container = new HBox(5);
                    container.getChildren().addAll(iconView, highlightedText);
                    setGraphic(container);
                    pseudoClassStateChanged(MATCHED, true);
                }
            }
        }

        private void determineIcon(TreeItem<String> treeItem) {
            if (treeItem == null) return;

            iconView.getStyleClass().removeAll("note-colored-icon", "file-colored-icon", "folder-colored-icon");

            if (!treeItem.isLeaf()) {
                iconView.getStyleClass().add("folder-colored-icon");
            } else {
                Path filePath = getFullPath(treeItem);
                if (fileSystemService.isMarkdownFile(filePath)) {
                    iconView.getStyleClass().add("note-colored-icon");
                } else {
                    iconView.getStyleClass().add("file-colored-icon");
                }
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

        private void finishRename(String newName) {
            String oldName = getItem();
            if (newName == null) newName = "";
            newName = newName.trim();
            if (newName.isEmpty() || newName.equals(oldName)) {
                cancelEdit();
                return;
            }

            var ti = getTreeItem();
            if (ti == null || ti.getParent() == null) {
                cancelEdit();
                return;
            }

            tryRename(ti, newName);
            commitEdit(newName);
        }
    }

    private record NameParts(String base, String ext) {
    }

    private NameParts splitName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            return new NameParts(fileName.substring(0, dot), fileName.substring(dot));
        }
        return new NameParts(fileName, "");
    }

    private void tryRename(TreeItem<String> ti, String newName) {
        Path oldAbs = getFullPath(ti);
        Path parent  = oldAbs.getParent();
        if (parent == null) {
            throw new TonpadBaseException("Cannot rename, because parent is null");
        }

        Path newAbs = parent.resolve(newName);
        Encryptor encoder = encryptorFactory.encryptorForKey();
        if (encoder.isActionWithNoPasswordAllowed(oldAbs)) {
            fileSystemService.rename(oldAbs.toString(), newAbs.toString());
            refreshTree();
            selectItem(newAbs, false);
            noteRenameHandler.accept(oldAbs, newAbs);
        }
        else {
            throw new DecryptionException("Invalid password");
        }
    }

    private static String norm(String s) {
        if (s == null) return "";
        s = s.replace('\\', '/');
        if (s.startsWith("./")) s = s.substring(2);
        return s;
    }

    private Node buildHighlightedName(String name, List<SearchService.Hit> hits) {
        var flow = new TextFlow();
        int i = 0, n = name.length();
        for (var h : hits) {
            int s = Math.max(0, Math.min(h.start(), n));
            int e = Math.max(0, Math.min(h.end(),   n));
            if (e <= s) continue;

            if (i < s) flow.getChildren().add(new Text(name.substring(i, s)));
            var t = new Text(name.substring(s, e));
            t.getStyleClass().add("file-tree-hit");
            flow.getChildren().add(t);
            i = e;
        }
        if (i < n) flow.getChildren().add(new Text(name.substring(i)));
        return flow;
    }


    private void onFileSelected(TreeItem<String> selectedItem) {
        this.selectedItem = selectedItem;
    }

    private TreeItem<String> convertFileTreeToTreeItem(FileTree fileTree) {
        Path path = fileTree.getPath();
        String fileName = path.getFileName() != null ? path.getFileName().toString() : path.toString();

        boolean isDirectory = fileTree.getChildren() != null;

        FileTreeItem treeItem = new FileTreeItem(fileName, isDirectory);

        treeItem.setValue(fileName);

        if (isDirectory && !fileTree.getChildren().isEmpty()) {
            for (FileTree child : fileTree.getChildren()) {
                TreeItem<String> childItem = convertFileTreeToTreeItem(child);
                fileTreeView.setEditable(true);
                treeItem.getChildren().add(childItem);
            }
        }

        return treeItem;
    }

    private Path addNote() {
        Path targetPath;

        if (selectedItem == null) {
            targetPath = vaultPathsContainer.getNotesPath();
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
            targetPath = vaultPathsContainer.getNotesPath();
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
        return fileTreeView.getSelectionModel().getSelectedItem();
    }

    private void setupShortCuts() {
        fileTreeVBox.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                attachAccelerator(newS, new KeyCodeCombination(KeyCode.DELETE), this::deleteSelected);

                attachAccelerator(newS, new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN), () -> onCopy(getSelected()));

                attachAccelerator(newS, new KeyCodeCombination(KeyCode.V, KeyCodeCombination.CONTROL_DOWN), () -> onPaste(getSelected()));

                attachAccelerator(newS, new KeyCodeCombination(KeyCode.X, KeyCodeCombination.CONTROL_DOWN), () -> onCut(getSelected()));

            }
        });
        fileTreeView.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
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

        var alert = new javafx.scene.control.Alert(
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
        noteCloseHandler.accept(fullPath);
    }

    private void deleteSelected() {
        var sel = fileTreeView.getSelectionModel().getSelectedItem();
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
        return "/ui/fxml/tree/file-tree-panel.fxml";
    }

}