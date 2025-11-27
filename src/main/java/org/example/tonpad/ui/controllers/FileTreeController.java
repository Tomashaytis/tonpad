package org.example.tonpad.ui.controllers;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
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

import org.example.tonpad.core.exceptions.TonpadBaseException;
import org.example.tonpad.core.files.Buffer;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.core.sort.SortKey;
import org.example.tonpad.core.sort.SortOptions;
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
    private HBox fileTreeToolsHBox;

    @FXML
    private Button addNoteButton;

    @FXML
    private Button addDirectoryButton;

    @FXML
    private Button addCollectionButton;

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
    private Consumer<Path> fileOpenHandler;

    @Setter
    private Map<String, List<SearchService.Hit>> hitsMap = Collections.emptyMap();

    private final Map<String, Boolean> expandedState = new HashMap<>();

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
                throw new IllegalStateException("sortMenu is null");
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
            Path newFilePath = addNote();
            refreshTree();
            selectItem(newFilePath, true);
            fileOpenHandler.accept(newFilePath);
        });
        
        addDirectoryButton.setOnAction(e -> {
            Path newDirPath = addDir();
            refreshTree();
            selectItem(newDirPath, true);
        });
        
        applySortOptions();
    }

    private void onSortPicked(SortKey picked)
    {
        if(sortKey != picked) sortKey = picked;
        applySortOptions();
        if(sortMenu != null && sortMenu.isShowing()) sortMenu.hide();
    }

    private void onSortFlagsChanged()
    {
    }
    
    private void applySortOptions()
    {
        refreshTree();        
    }

    private void onOpenFile(TreeItem<String> target) {
        if (target != null && target.isLeaf()) {
            Path filePath = getFullPath(target);
            if (fileOpenHandler != null) {
                fileOpenHandler.accept(filePath);
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
            parts.add(0, cur.getValue());
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
                if (ti != null && ti.isLeaf())
                    onOpenFile(ti);
                e.consume();
            }
        });
    }

    private boolean canPasteInto(TreeItem<String> ti) {
        if (ti == null) return false;
        Path p = getFullPath(ti);
        if (!ti.isLeaf()) return Files.isDirectory(p);
        var parent = p.getParent();
        return parent != null && Files.isDirectory(parent);
    }

    private ContextMenu buildContextMenu(TreeCell<String> cell)
    {
        MenuItem copy = new MenuItem("Copy (Ctrl+C)");
        copy.setOnAction(e -> onCopy(cell.getTreeItem()));

        MenuItem cut = new MenuItem("Cut (Ctrl+X)");
        cut.setOnAction(e -> onCut(cell.getTreeItem()));

        MenuItem paste = new MenuItem("Paste (Ctrl+V)");
        paste.setOnAction(e -> onPaste(cell.getTreeItem()));

        MenuItem copyTonpadUrl = new MenuItem("Copy vault path");
        copyTonpadUrl.setOnAction(e -> onCopyVaultPath());

        MenuItem copyAbsPath = new MenuItem("Copy absolute path");
        copyAbsPath.setOnAction(e -> onCopyAbsPath(cell.getTreeItem()));

        MenuItem copyRelPath = new MenuItem("Copy relative path");
        copyRelPath.setOnAction(e -> onCopyRelPath(cell.getTreeItem()));

        MenuItem showInExplorer = new MenuItem("Show in explorer");
        showInExplorer.setOnAction(e -> onShowInExplorer(cell.getTreeItem()));

        MenuItem rename = new MenuItem("Rename (F2)");
        rename.setOnAction(e ->
        {
            onRename(cell.getTreeItem());}
        );

        MenuItem del = new MenuItem("Delete");
        del.setOnAction(e -> confirmAndDeleteForItem(cell.getTreeItem()));

        return new ContextMenu(
            copy,
            cut,
            paste,
            new SeparatorMenuItem(),
            copyTonpadUrl,
            copyAbsPath,
            copyRelPath,
            new SeparatorMenuItem(),
            showInExplorer,
            new SeparatorMenuItem(),
            rename,
            del
        );
    }

    private void onCopy(TreeItem<String> node) 
    {
        if (node != null) {
            fileSystemService.copyFile(getFullPath(node));
        }
    }
    private void onCut(TreeItem<String> node) 
    {
        if (node != null) {
            fileSystemService.cutFile(getFullPath(node));
        }
    }
    private void onPaste(TreeItem<String> target) 
    {
        if(target != null)
        {
            if(Files.isDirectory(getFullPath(target)))
            {
                fileSystemService.pasteFile(getFullPath(target));
                refreshTree();
            }
            else
            {
                fileSystemService.pasteFile(getFullPath(target.getParent()));
                refreshTree();
            }
        }
    }

    private Path resolveTargetDirForPaste(TreeItem<String> node) {
        if (node == null) return vaultPathsContainer.getNotesPath();
        Path here = getFullPath(node);
        return Files.isDirectory(here) ? here : (here.getParent() != null ? here.getParent() : vaultPathsContainer.getNotesPath());
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
            cc.putString(getRelativePath(node));
            Clipboard.getSystemClipboard().setContent(cc);
        }
    }

    private void onShowInExplorer(TreeItem<String> node) {
        if (node != null) {
            fileSystemService.showFileInExplorer(getFullPath(node));
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
        private ContextMenu menu;
        private TextField editor;

        public EditableFileCell() {
            setEditable(true);
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
                setContextMenu(null);
                return;
            }

            if (isEditing()) {
                if (editor == null) editor = createEditor();
                editor.setText(item);
                setText(null);
                setGraphic(editor);
            } else {
                String rel = norm(getRelativePath(getTreeItem()));
                var ranges = (hitsMap != null) ? hitsMap.get(rel) : null;
                if (ranges == null || ranges.isEmpty()) {
                    setGraphic(null);
                    setText(item);
                    pseudoClassStateChanged(MATCHED, false);
                } else {
                    setText(null);
                    setGraphic(buildHighlightedName(item, ranges));
                    pseudoClassStateChanged(MATCHED, true);
                }

                if (menu == null) menu = buildContextMenu(this);

                boolean isRoot = getTreeItem() != null && getTreeItem().getParent() == null;
                for (MenuItem mi : menu.getItems()) {
                    String id = mi.getId();
                    if (id == null) continue;
                    if (id.equals("ctxRename") || id.equals("ctxDel")) mi.setDisable(isRoot);
                    if (id.equals("ctxPaste")) {
                        boolean canPasteHere = canPasteInto(getTreeItem());
                        boolean hasClipboard = buffer != null && buffer.getCopyBuffer() != null && !buffer.getCopyBuffer().isEmpty();
                        mi.setDisable(!(canPasteHere && hasClipboard));
                    }
                }
                setContextMenu(menu);
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
            setGraphic(null);
        }

        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            setGraphic(null);
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

        fileSystemService.rename(oldAbs.toString(), newAbs.toString());

        refreshTree();
        selectItem(newAbs, false);
    }

    private static String norm(String s) {
        if (s == null) return "";
        s = s.replace('\\', '/');
        if (s.startsWith("./")) s = s.substring(2);
        return s;
    }

    private javafx.scene.Node buildHighlightedName(String name, List<SearchService.Hit> hits) {
        var flow = new TextFlow();
        int i = 0, n = name.length();
        for (var h : hits) {
            int s = Math.max(0, Math.min(h.start(), n));
            int e = Math.max(0, Math.min(h.end(),   n));
            if (e <= s) continue;

            if (i < s) flow.getChildren().add(new Text(name.substring(i, s)));
            var t = new Text(name.substring(s, e));
            t.getStyleClass().add("filetree-hit");
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

        return  treeItem;
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
        if (node == null) return;
        if (node.getParent() == null) return;

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

        fileSystemService.delete(getFullPath(node));
        refreshTree();
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
        return "/ui/fxml/file-tree-panel.fxml";
    }

}