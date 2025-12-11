package org.example.tonpad.core.editor;

import javafx.scene.web.WebEngine;
import org.example.tonpad.core.editor.dto.SearchResult;
import org.example.tonpad.core.editor.enums.FormatType;
import org.example.tonpad.core.editor.enums.LinkType;
import org.example.tonpad.core.editor.enums.ParagraphType;
import org.example.tonpad.core.editor.listener.FrontMatterChangeListener;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для работы с Markdown редактором в JavaFX
 * Инкапсулирует все вызовы JavaScript API
 */
public interface Editor {

    void setNoteContent(String noteContent);

    void setFrontMatter(Map<String, String> frontMatter);

    void insertSnippet(String snippetContent);

    CompletableFuture<SearchResult> find(String query);

    CompletableFuture<SearchResult> findNext();

    CompletableFuture<SearchResult> findPrevious();

    CompletableFuture<SearchResult> clearSearch();

    CompletableFuture<SearchResult> goTo(int number);

    CompletableFuture<String> getNoteContent();

    CompletableFuture<String> getFrontMatter(boolean jsonFormat);

    CompletableFuture<String> getDoc();

    CompletableFuture<String> getHtml();

    CompletableFuture<String> getMarkdown();

    void link(LinkType linkType);

    void format(FormatType formatType);

    void paragraph(ParagraphType paragraphType);

    void insert(String content);

    CompletableFuture<Boolean> canCreateLinks();

    void copy();

    void cut();

    void paste();

    void selectAll();

    void focus();

    void destroy();

    URL getEditorHtmlSource();

    URL getEditorCssSource();

    URL getEditorJsSource();

    void addFrontMatterChangeListener(FrontMatterChangeListener listener);

    void removeFrontMatterChangeListener(FrontMatterChangeListener listener);
}