package org.example.tonpad.ui.controllers.toolbar;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import lombok.Setter;
import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.core.editor.enums.FormatType;
import org.example.tonpad.core.editor.enums.LinkType;
import org.example.tonpad.core.editor.enums.ParagraphType;
import org.example.tonpad.ui.controllers.AbstractController;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;


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

    @Setter
    private WebView webView;

    public void init(AnchorPane parent) {
        if (root != null) {
            root.setVisible(false);
            root.setManaged(false);
        }

        parent.getChildren().add(root);
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
        if (root == null || root.getParent() == null) return;

        editor.canCreateLinks().thenAccept(result -> {
            if (result) {
                addNoteLinkButton.setDisable(false);
                addExternalLinkButton.setDisable(false);
            } else {
                addNoteLinkButton.setDisable(true);
                addExternalLinkButton.setDisable(true);
            }

            root.setVisible(true);
            root.setManaged(true);

            Platform.runLater(() -> {
                root.applyCss();
                root.layout();

                double panelWidth = root.getWidth();
                double panelHeight = root.getHeight();

                AnchorPane parent = (AnchorPane) root.getParent();

                Scene scene = parent.getScene();
                if (scene == null) return;

                Window window = scene.getWindow();
                if (window == null) return;

                Point2D sceneCoords = scene.getRoot().screenToLocal(screenX, screenY);
                if (sceneCoords == null) return;

                Point2D localCoords = parent.sceneToLocal(sceneCoords);
                if (localCoords == null) return;

                double parentWidth = parent.getWidth();
                double parentHeight = parent.getHeight();

                double x = localCoords.getX();
                double y = localCoords.getY();

                double preferredX = x;
                double preferredY = y + 10;

                if (y - panelHeight >= 0) {
                    preferredY = y - panelHeight - 10;
                } else {
                    preferredY = Math.max(0, (parentHeight - panelHeight) / 2);
                }

                if (preferredX + panelWidth > parentWidth) {
                    preferredX = parentWidth - panelWidth;
                }
                if (preferredX < 0) {
                    preferredX = 0;
                }

                preferredX = Math.max(0, Math.min(preferredX, parentWidth - panelWidth));
                preferredY = Math.max(0, Math.min(preferredY, parentHeight - panelHeight));

                root.setLayoutX(preferredX);
                root.setLayoutY(preferredY);

                setupOutsideClickHandler();
            });
        });
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
                Point2D scenePoint = new Point2D(event.getSceneX(), event.getSceneY());
                Point2D parentLocalPoint = root.getParent().sceneToLocal(scenePoint);

                if (parentLocalPoint != null) {
                    boolean inside = root.getBoundsInParent().contains(parentLocalPoint);

                    if (!inside) {
                        hide();
                    }
                }
            });
        }
    }

    private void onAddNoteLink() {
        editor.link(LinkType.NOTE_LINK);
        hide();
        webView.requestFocus();
    }

    private void onAddExternalLink() {
        editor.link(LinkType.EXTERNAL_LINK);
        hide();
        webView.requestFocus();
    }

    private void onFormatBold() {
        editor.format(FormatType.BOLD);
        hide();
        webView.requestFocus();
    }

    private void onFormatItalic() {
        editor.format(FormatType.ITALIC);
        hide();
        webView.requestFocus();
    }

    private void onFormatUnderline() {
        editor.format(FormatType.UNDERLINE);
        hide();
        webView.requestFocus();
    }

    private void onFormatStrikethrough() {
        editor.format(FormatType.STRIKETHROUGH);
        hide();
        webView.requestFocus();
    }

    private void onFormatHighlight() {
        editor.format(FormatType.HIGHLIGHT);
        hide();
        webView.requestFocus();
    }

    private void onFormatComment() {
        editor.format(FormatType.COMMENT);
        hide();
        webView.requestFocus();
    }

    private void onFormatCode() {
        editor.format(FormatType.CODE);
        hide();
        webView.requestFocus();
    }

    private void onFormatMath() {
        editor.format(FormatType.MATH);
        hide();
        webView.requestFocus();
    }

    private void onClearFormatting() {
        editor.format(FormatType.CLEAR);
        hide();
        webView.requestFocus();
    }

    private void onBulletList() {
        editor.paragraph(ParagraphType.BULLET_LIST);
        hide();
        webView.requestFocus();
    }

    private void onOrderedList() {
        editor.paragraph(ParagraphType.ORDERED_LIST);
        hide();
        webView.requestFocus();
    }

    private void onHeading1() {
        editor.paragraph(ParagraphType.HEADING_1);
        hide();
        webView.requestFocus();
    }

    private void onHeading2() {
        editor.paragraph(ParagraphType.HEADING_2);
        hide();
        webView.requestFocus();
    }

    private void onHeading3() {
        editor.paragraph(ParagraphType.HEADING_3);
        hide();
        webView.requestFocus();
    }

    private void onHeading4() {
        editor.paragraph(ParagraphType.HEADING_4);
        hide();
        webView.requestFocus();
    }

    private void onHeading5() {
        editor.paragraph(ParagraphType.HEADING_5);
        hide();
        webView.requestFocus();
    }

    private void onHeading6() {
        editor.paragraph(ParagraphType.HEADING_6);
        hide();
        webView.requestFocus();
    }

    private void onBodyText() {
        editor.paragraph(ParagraphType.BODY);
        hide();
        webView.requestFocus();
    }

    private void onQuote() {
        editor.paragraph(ParagraphType.QUOTE);
        hide();
        webView.requestFocus();
    }

    private void onInsertSnippet() {
        System.out.println("Insert snippet clicked");
        hide();
        webView.requestFocus();
    }

    private void onInsertHorizontalRule() {
        editor.insert("---\n");
        hide();
        webView.requestFocus();
    }

    private void onCut() {
        editor.cut();
        hide();
        webView.requestFocus();
    }

    private void onCopy() {
        editor.copy();
        hide();
        webView.requestFocus();
    }

    private void onPaste() {
        editor.paste();
        hide();
        webView.requestFocus();
    }

    private void onSelectAll() {
        editor.selectAll();
        hide();
        webView.requestFocus();
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/toolbar/editor-toolbar.fxml";
    }

}