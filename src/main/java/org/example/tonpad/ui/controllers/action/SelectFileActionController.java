package org.example.tonpad.ui.controllers.action;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import org.example.tonpad.ui.controllers.AbstractController;
import org.springframework.stereotype.Component;


@Component
@Getter
public class SelectFileActionController extends AbstractController {

    @FXML
    private MenuItem openInCurrentTabMenuItem;

    @FXML
    private MenuItem newNoteMenuItem;

    @FXML
    private MenuItem newFolderMenuItem;

    @FXML
    private MenuItem copyMenuItem;

    @FXML
    private MenuItem cutMenuItem;

    @FXML
    private MenuItem pasteMenuItem;

    @FXML
    private MenuItem copyVaultPathMenuItem;

    @FXML
    private MenuItem copyAbsolutePathMenuItem;

    @FXML
    private MenuItem copyRelativePathMenuItem;

    @FXML
    private MenuItem showInNotepadMenuItem;

    @FXML
    private MenuItem showInExplorerMenuItem;

    @FXML
    private MenuItem renameMenuItem;

    @FXML
    private MenuItem removeMenuItem;

    @FXML
    private ContextMenu contextMenu;

    public ContextMenu createContextMenu() {
        return contextMenu;
    }
    protected String getFxmlSource() {
        return "/ui/fxml/action/select-file-action.fxml";
    }
}