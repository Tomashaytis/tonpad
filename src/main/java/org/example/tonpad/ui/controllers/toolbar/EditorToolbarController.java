package org.example.tonpad.ui.controllers.toolbar;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import lombok.Setter;
import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.ui.controllers.AbstractController;
import org.springframework.stereotype.Component;

@Component
public class EditorToolbarController extends AbstractController {

    @FXML
    private VBox root;

    @FXML
    private Button addNoteLinkButton;

    @FXML
    private Button addExternalLinkButton;

    @FXML
    private MenuButton formatMenuButton;

    @FXML
    private MenuItem boldMenuItem;

    @FXML
    private MenuItem italicMenuItem;

    @FXML
    private MenuItem underlineMenuItem;

    @FXML
    private MenuItem strikethroughMenuItem;

    @FXML
    private MenuItem highlightMenuItem;

    @FXML
    private MenuItem commentMenuItem;

    @FXML
    private MenuItem codeMenuItem;

    @FXML
    private MenuItem mathMenuItem;

    @FXML
    private MenuItem clearFormattingMenuItem;

    @FXML
    private MenuButton paragraphMenuButton;

    @FXML
    private MenuItem bulletListMenuItem;

    @FXML
    private MenuItem orderedListMenuItem;

    @FXML
    private MenuItem heading1MenuItem;

    @FXML
    private MenuItem heading2MenuItem;

    @FXML
    private MenuItem heading3MenuItem;

    @FXML
    private MenuItem heading4MenuItem;

    @FXML
    private MenuItem heading5MenuItem;

    @FXML
    private MenuItem heading6MenuItem;

    @FXML
    private MenuItem bodyTextMenuItem;

    @FXML
    private MenuItem quoteMenuItem;

    @FXML
    private MenuButton insertMenuButton;

    @FXML
    private MenuItem insertSnippetMenuItem;

    @FXML
    private MenuItem insertHorizontalRuleMenuItem;

    @FXML
    private Button cutButton;

    @FXML
    private Button copyButton;

    @FXML
    private Button pasteButton;

    @FXML
    private Button selectAllButton;

    @Setter
    private Editor editor;

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/toolbar/editor-toolbar.fxml";
    }

    @FXML
    public void initialize() {
        if (root != null) {
            root.setVisible(false);
            root.setManaged(false);
        }
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        addNoteLinkButton.setOnAction(event -> onAddNoteLink());
        addExternalLinkButton.setOnAction(event -> onAddExternalLink());
        boldMenuItem.setOnAction(event -> onFormatBold());
        italicMenuItem.setOnAction(event -> onFormatItalic());
        underlineMenuItem.setOnAction(event -> onFormatUnderline());
        strikethroughMenuItem.setOnAction(event -> onFormatStrikethrough());
        highlightMenuItem.setOnAction(event -> onFormatHighlight());
        commentMenuItem.setOnAction(event -> onFormatComment());
        codeMenuItem.setOnAction(event -> onFormatCode());
        mathMenuItem.setOnAction(event -> onFormatMath());
        clearFormattingMenuItem.setOnAction(event -> onClearFormatting());
        bulletListMenuItem.setOnAction(event -> onBulletList());
        orderedListMenuItem.setOnAction(event -> onOrderedList());
        heading1MenuItem.setOnAction(event -> onHeading1());
        heading2MenuItem.setOnAction(event -> onHeading2());
        heading3MenuItem.setOnAction(event -> onHeading3());
        heading4MenuItem.setOnAction(event -> onHeading4());
        heading5MenuItem.setOnAction(event -> onHeading5());
        heading6MenuItem.setOnAction(event -> onHeading6());
        bodyTextMenuItem.setOnAction(event -> onBodyText());
        quoteMenuItem.setOnAction(event -> onQuote());
        insertSnippetMenuItem.setOnAction(event -> onInsertSnippet());
        insertHorizontalRuleMenuItem.setOnAction(event -> onInsertHorizontalRule());
        cutButton.setOnAction(event -> onCut());
        copyButton.setOnAction(event -> onCopy());
        pasteButton.setOnAction(event -> onPaste());
        selectAllButton.setOnAction(event -> onSelectAll());
    }

    public void showAt(double screenX, double screenY) {
        if (root == null) return;

        root.setVisible(true);
        root.setManaged(true);

        if (root.getScene() != null && root.getScene().getWindow() != null) {
            root.getScene().getWindow().sizeToScene();
            root.setLayoutX(screenX - root.getScene().getWindow().getX());
            root.setLayoutY(screenY - root.getScene().getWindow().getY());
        }

        setupOutsideClickHandler();
    }

    public void hide() {
        if (root != null) {
            root.setVisible(false);
            root.setManaged(false);
        }
    }

    private void setupOutsideClickHandler() {
        if (root.getScene() != null) {
            root.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (!root.getBoundsInParent().contains(event.getX(), event.getY())) {
                    hide();
                }
            });
        }
    }

    private void onAddNoteLink() {
        System.out.println("Add note link clicked");
    }

    private void onAddExternalLink() {
        System.out.println("Add external link clicked");
    }

    private void onFormatBold() {
        System.out.println("Format bold clicked");
    }

    private void onFormatItalic() {
        System.out.println("Format italic clicked");
    }

    private void onFormatUnderline() {
        System.out.println("Format underline clicked");
    }

    private void onFormatStrikethrough() {
        System.out.println("Format strikethrough clicked");
    }

    private void onFormatHighlight() {
        System.out.println("Format highlight clicked");
    }

    private void onFormatComment() {
        System.out.println("Format comment clicked");
    }

    private void onFormatCode() {
        System.out.println("Format code clicked");
    }

    private void onFormatMath() {
        System.out.println("Format math clicked");
    }

    private void onClearFormatting() {
        System.out.println("Clear formatting clicked");
    }

    private void onBulletList() {
        System.out.println("Bullet list clicked");
    }

    private void onOrderedList() {
        System.out.println("Ordered list clicked");
    }

    private void onHeading1() {
        System.out.println("Heading 1 clicked");
    }

    private void onHeading2() {
        System.out.println("Heading 2 clicked");
    }

    private void onHeading3() {
        System.out.println("Heading 3 clicked");
    }

    private void onHeading4() {
        System.out.println("Heading 4 clicked");
    }

    private void onHeading5() {
        System.out.println("Heading 5 clicked");
    }

    private void onHeading6() {
        System.out.println("Heading 6 clicked");
    }

    private void onBodyText() {
        System.out.println("Body text clicked");
    }

    private void onQuote() {
        System.out.println("Quote clicked");
    }

    private void onInsertSnippet() {
        System.out.println("Insert snippet clicked");
    }

    private void onInsertHorizontalRule() {
        System.out.println("Insert horizontal rule clicked");
    }

    private void onCut() {
        System.out.println("Cut clicked");
    }

    private void onCopy() {
        System.out.println("Copy clicked");
    }

    private void onPaste() {
        System.out.println("Paste clicked");
    }

    private void onSelectAll() {
        System.out.println("Select all clicked");
    }
}