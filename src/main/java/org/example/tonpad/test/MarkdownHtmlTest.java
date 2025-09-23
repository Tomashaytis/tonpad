package org.example.tonpad.test;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import lombok.SneakyThrows;
import org.example.tonpad.parser.extension.block.immutable.ImmutableExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MarkdownHtmlTest {

    private static final MutableDataSet OPTIONS = new MutableDataSet();

    static {
        OPTIONS.set(FlexmarkHtmlConverter.RENDER_COMMENTS, true);

        List<Extension> extensions = new ArrayList<>();
        extensions.add(new ImmutableExtension());

        OPTIONS.set(Parser.EXTENSIONS, extensions);
    }

    private static final Parser PARSER = Parser.builder(OPTIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();
    private static final FlexmarkHtmlConverter HTML_CONVERTER = FlexmarkHtmlConverter.builder(OPTIONS).build();

    @SneakyThrows
    public static String toHtml() {
        String markdownContent = Files.readString(Path.of("src/main/resources/test.md"));
        Node document = PARSER.parse(markdownContent);
        String html = RENDERER.render(document);

        return html;
    }

    public static String toMarkdown(String html) {
        return HTML_CONVERTER.convert(html);
    }

}
