package org.example.tonpad.ui.controllers.toolbar;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import lombok.Setter;
import org.example.tonpad.core.editor.Editor;
import org.example.tonpad.core.editor.enums.FormatType;
import org.example.tonpad.core.editor.enums.LinkType;
import org.example.tonpad.core.editor.enums.ParagraphType;
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

    @Setter
    private WebView webView;

    @FXML
    public void initialize() {
        if (root != null) {
            root.setVisible(false);
            root.setManaged(false);
        }
        setupEventHandlers();
    }

    public void init(AnchorPane parent) {
        parent.getChildren().add(root);
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
                Point2D localCoords = root.getParent().screenToLocal(screenX, screenY);
                if (localCoords == null) return;

                double panelWidth = root.getWidth();
                double panelHeight = root.getHeight();
                double parentWidth = root.getParent().getBoundsInLocal().getWidth();
                double parentHeight = root.getParent().getBoundsInLocal().getHeight();

                Point2D[] possiblePositions = {
                        new Point2D(localCoords.getX(), localCoords.getY()),

                        new Point2D(localCoords.getX(), localCoords.getY() - panelHeight),

                        new Point2D(localCoords.getX() - panelWidth, localCoords.getY()),

                        new Point2D(localCoords.getX() - panelWidth, localCoords.getY() - panelHeight),

                        new Point2D(localCoords.getX() - panelWidth / 2, localCoords.getY()),

                        new Point2D(localCoords.getX() - panelWidth / 2, localCoords.getY() - panelHeight)
                };

                Point2D bestPosition = null;
                for (Point2D pos : possiblePositions) {
                    if (isPositionValid(pos.getX(), pos.getY(), panelWidth, panelHeight,
                            parentWidth, parentHeight)) {
                        bestPosition = pos;
                        break;
                    }
                }

                // Если ничего не подошло, используем принудительное позиционирование
                if (bestPosition == null) {
                    bestPosition = getForcedPosition(localCoords, panelWidth, panelHeight,
                            parentWidth, parentHeight);
                }

                root.setLayoutX(bestPosition.getX());
                root.setLayoutY(bestPosition.getY());

                setupOutsideClickHandler();
            });
        });
    }

    private boolean isPositionValid(double x, double y, double width, double height,
                                    double parentWidth, double parentHeight) {
        return x >= 0 &&
                y >= 0 &&
                x + width <= parentWidth &&
                y + height <= parentHeight;
    }

    private Point2D getForcedPosition(Point2D clickPoint, double width, double height,
                                      double parentWidth, double parentHeight) {
        double x = clickPoint.getX();
        double y = clickPoint.getY();

        if (x + width > parentWidth) {
            x = parentWidth - width;
        }
        if (x < 0) {
            x = 0;
        }

        if (y + height > parentHeight) {
            y = parentHeight - height;
        }
        if (y < 0) {
            y = 0;
        }

        return new Point2D(x, y);
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