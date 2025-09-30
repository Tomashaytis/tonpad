package org.example.tonpad.core.service.impl;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.service.MarkdownService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Node;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkdownServiceImpl implements MarkdownService {

    private static final String STYLED_HTML_FROMATED_STRING = """
            <html><head><style>
            body { font-family: Arial, sans-serif; padding: 20px; }
            h1, h2, h3 { color: #333; }
            pre { background: #f4f4f4; padding: 10px; border-radius: 5px; }
            code { font-family: 'Courier New', monospace; }
            </style></head><body contenteditable='true'>%s</body></html>
            """;

    private final MutableDataSet options = new MutableDataSet();

    private final List<Extension> extensions;

    private Parser parser;
    private HtmlRenderer renderer;
    private FlexmarkHtmlConverter htmlConverter;

    @PostConstruct
    private void postConstruct() {
        options.set(FlexmarkHtmlConverter.RENDER_COMMENTS, false);

        options.set(Parser.EXTENSIONS, extensions);

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
        htmlConverter = FlexmarkHtmlConverter.builder(options).build();
    }

    @Override
    public Document parseMarkdownFile(String file) {
        return parser.parse(file);
    }

    @Override
    public String renderMarkdownFileToHtml(Document document) {
        return String.format(STYLED_HTML_FROMATED_STRING, renderer.render(document));
    }

    @Override
    public String convertHtmlToMarkdown(String html) {
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        Map<String, String> commentPlaceholders = new HashMap<>();
        Set<UUID> uuids = new HashSet<>();

        for (Node node : doc.select("*").stream().flatMap(n -> n.childNodes().stream()).toList()) {
            if (node instanceof Comment comment) {
                UUID uuid = UUID.randomUUID();
                while (!uuids.add(uuid)) {
                    uuid = UUID.randomUUID();
                }

                String placeholder = "CUSTOM_TAG_PLACEHOLDER_" + uuid;
                commentPlaceholders.put(placeholder, "<!--" + comment.getData() + "-->");
                node.replaceWith(new org.jsoup.nodes.TextNode(placeholder));
            }
        }

        String processedHtml = doc.html();
        String markdown = htmlConverter.convert(processedHtml);

        for (Map.Entry<String, String> entry : commentPlaceholders.entrySet()) {
            markdown = markdown.replace(entry.getKey(), entry.getValue());
        }

        return markdown;
    }

}
