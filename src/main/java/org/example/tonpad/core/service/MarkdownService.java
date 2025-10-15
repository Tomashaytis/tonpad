package org.example.tonpad.core.service;

import com.vladsch.flexmark.util.ast.Document;

public interface MarkdownService {

    Document parseMarkdownFile(String file);

    String renderMarkdownFileToHtml(Document document);

    String renderMarkdownFileToHtmlPage(Document document);

    String convertHtmlToMarkdown(String html);

}
