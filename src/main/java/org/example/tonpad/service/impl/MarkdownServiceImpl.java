package org.example.tonpad.service.impl;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.service.MarkdownService;
import org.springframework.stereotype.Service;

import java.util.List;

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
        options.set(FlexmarkHtmlConverter.RENDER_COMMENTS, true);
        options.set(FlexmarkHtmlConverter.COMMENT_ORIGINAL_NON_NUMERIC_LIST_ITEM, true);

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
        return htmlConverter.convert(html);
    }

}
