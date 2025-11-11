package org.example.tonpad.ui.controllers;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.example.tonpad.core.files.Buffer;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.example.tonpad.core.models.SortKey;
import org.example.tonpad.core.models.SortOptions;
import org.example.tonpad.core.service.SearchService;
import org.example.tonpad.ui.extentions.FileTreeItem;
import org.example.tonpad.ui.extentions.VaultPath;
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

    private final VaultPath vaultPath;

    private static final javafx.css.PseudoClass MATCHED = javafx.css.PseudoClass.getPseudoClass("matched");

    private TreeItem<String> rootItem;

    private TreeItem<String> selectedItem;

    private boolean isCollapsed = true;

    private final BooleanProperty programmaticEdit = new SimpleBooleanProperty(false);

    @Setter
    private Consumer<String> fileOpenHandler;

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
        miNameDesc.setUserData(SortKey.NAME_DESC); // при смене порядка меню убирается и рефреш сразу, при выборе нижних двух не закрывается сразу, а по ESC, и не обновляется, вручную надо рефрешить, или изменить порядок

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
            String newFilePath = addNote();
            refreshTree();
            selectItem(newFilePath);
            fileOpenHandler.accept(newFilePath);
        });
        
        addDirectoryButton.setOnAction(e -> {
            String dirPath = addDir();
            refreshTree();
            selectItem(dirPath);
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

    private void onOpenFile() {
        if (selectedItem != null && selectedItem.isLeaf()) {
            String filePath = getFullPath(selectedItem);
            if (fileOpenHandler != null) {
                fileOpenHandler.accept(filePath);
            }
        }
    }

    private void selectItem(String path) {
        Path relativePath = Path.of(vaultPath.getVaultPath()).relativize(Path.of(path));
        TreeItem<String> item = findTreeItemByPath(rootItem, relativePath);
        if (item != null) {
            fileTreeView.getSelectionModel().select(item);
            selectedItem = item;
            fileTreeView.scrollTo(fileTreeView.getRow(item));
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
        FileTree fileTree = fileSystemService.getFileTreeSorted(vaultPath.getVaultPath(), opt);
        TreeItem<String> newRoot = convertFileTreeToTreeItem(fileTree);

        fileTreeView.setRoot(newRoot);

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
            parts.add(0, cur.getValue()); // корень тоже включаем
        }
        return String.join("/", parts).replace('\\','/');
    }

    private String getFullPath(TreeItem<String> item) {
        if (item == null) {
            return vaultPath.getVaultPath();
        }

        List<String> pathSegments = new ArrayList<>();
        TreeItem<String> current = item;

        while (current.getParent() != null) {
            pathSegments.addFirst(current.getValue());
            current = current.getParent();
        }

        return Path.of(vaultPath.getVaultPath(), pathSegments.toArray(new String[0])).toString();
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
        FileTree fileTree = fileSystemService.getFileTree(vaultPath.getVaultPath());

        rootItem = convertFileTreeToTreeItem(fileTree);

        fileTreeView.setRoot(rootItem);

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
        fileTreeView.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {

            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && e.getClickCount() == 2) {
                var ti = fileTreeView.getSelectionModel().getSelectedItem();
                if (ti != null && ti.isLeaf()) onOpenFile();
                e.consume();
            }
        });
    }

    private boolean canPasteInto(TreeItem<String> ti) {
        if (ti == null) return false;
        java.nio.file.Path p = java.nio.file.Path.of(getFullPath(ti));
        if (!ti.isLeaf()) return java.nio.file.Files.isDirectory(p);
        var parent = p.getParent();
        return parent != null && java.nio.file.Files.isDirectory(parent);
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
            if(Files.isDirectory(Path.of(getFullPath(target))))
            {
                fileSystemService.pasteFile(Path.of(getFullPath(target)));
                refreshTree();
            }
            else
            {
                fileSystemService.pasteFile(Path.of(getFullPath(target.getParent())));
                refreshTree();
            }
        }
    }

    private Path resolveTargetDirForPaste(TreeItem<String> node) {
        if (node == null) return Path.of(vaultPath.getVaultPath());
        Path here = Path.of(getFullPath(node));
        return Files.isDirectory(here) ? here : (here.getParent() != null ? here.getParent() : Path.of(vaultPath.getVaultPath()));
    }

    private void onCopyVaultPath() 
    {
        fileSystemService.copyVaultPath();
    }

    private void onCopyAbsPath(TreeItem<String> node) {
        if (node != null) {
            fileSystemService.copyAbsFilePath(getFullPath(node));
        }
    }

    private void onCopyRelPath(TreeItem<String> node) 
    {
        if (node != null)
        {
            fileSystemService.copyRelFilePath(getRelativePath(node));
        }
    }

    private void onShowInExplorer(TreeItem<String> node) 
    {
        if (node != null)
        {   
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

    private final class EditableFileCell extends TreeCell<String> 
    {
        private ContextMenu menu;
        private javafx.scene.control.TextField editor;

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

        private javafx.scene.control.TextField createEditor() {
            var tf = new javafx.scene.control.TextField(getItem());

            tf.setOnAction(ev -> finishRename(tf.getText()));

            tf.setOnKeyPressed(ke -> {
                if (ke.getCode() == javafx.scene.input.KeyCode.ESCAPE) cancelEdit();
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
                        if (Files.isDirectory(Path.of(getFullPath(getTreeItem())))) {
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
            if (newName.isEmpty() || newName.equals(oldName)) { cancelEdit(); return; }

            var ti = getTreeItem();
            if (ti == null || ti.getParent() == null) { cancelEdit(); return; }

            boolean ok = tryRename(ti, newName);
            if (ok) {
                commitEdit(newName);
            } else {
                cancelEdit();
            }
        }
    }

private static final class NameParts {
    final String base, ext;
    NameParts(String base, String ext) { this.base = base; this.ext = ext; }
}

private NameParts splitName(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot > 0 && dot < fileName.length() - 1) {
        return new NameParts(fileName.substring(0, dot), fileName.substring(dot));
    }
    return new NameParts(fileName, "");
}

private boolean tryRename(TreeItem<String> ti, String newName) {
    try {
        Path oldAbs = Path.of(getFullPath(ti));
        Path parent  = oldAbs.getParent();
        if (parent == null) return false;

        Path newAbs = parent.resolve(newName);

        fileSystemService.rename(oldAbs.toString(), newAbs.toString());

        refreshTree();
        selectItem(newAbs.toString());
        return true;
    } catch (Exception ex) {
        showError("Rename error", String.valueOf(ex.getMessage()));
        return false;
    }
}

private void showError(String header, String msg) {
    var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, msg, javafx.scene.control.ButtonType.OK);
    var owner = fileTreeVBox.getScene() != null ? fileTreeVBox.getScene().getWindow() : null;
    if (owner != null) alert.initOwner(owner);
    alert.setHeaderText(header);
    alert.showAndWait();
}



    private static String norm(String s) {
        if (s == null) return "";
        s = s.replace('\\', '/');
        if (s.startsWith("./")) s = s.substring(2);
        return s;
    }

    private javafx.scene.Node buildHighlightedName(String name, java.util.List<SearchService.Hit> hits) {
        // предполагаем, что hits отсортированы и не пересекаются
        var flow = new javafx.scene.text.TextFlow();
        int i = 0, n = name.length();
        for (var h : hits) {
            int s = Math.max(0, Math.min(h.start(), n));
            int e = Math.max(0, Math.min(h.end(),   n));
            if (e <= s) continue;

            if (i < s) flow.getChildren().add(new javafx.scene.text.Text(name.substring(i, s)));
            var t = new javafx.scene.text.Text(name.substring(s, e));
            t.getStyleClass().add("filetree-hit");   // стиль подсвеченного фрагмента
            flow.getChildren().add(t);
            i = e;
        }
        if (i < n) flow.getChildren().add(new javafx.scene.text.Text(name.substring(i)));
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

    private String addNote() {
        Path targetPath;

        if (selectedItem == null) {
            targetPath = Path.of(vaultPath.getVaultPath());
        } else {
            String fullPath = getFullPath(selectedItem);
            Path selectedPath = Path.of(fullPath);

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
        return filePath.toString();
    }

    private String addDir() {
        Path targetPath;

        if (selectedItem == null) {
            targetPath = Path.of(vaultPath.getVaultPath());
        } else {
            String fullPath = getFullPath(selectedItem);
            Path selectedPath = Path.of(fullPath);

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

        return dirPath.toString();
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
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Delete selected file or folder?",
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );
        if (owner != null) alert.initOwner(owner);

        var res = alert.showAndWait();
        if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.OK) return;

        fileSystemService.delete(Path.of(getFullPath(node)).toString());
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