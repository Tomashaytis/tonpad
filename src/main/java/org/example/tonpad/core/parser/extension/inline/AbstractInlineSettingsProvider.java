package org.example.tonpad.core.parser.extension.inline;

import com.vladsch.flexmark.util.ast.Node;
import lombok.Getter;
import org.example.tonpad.core.service.MarkdownService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.function.Function;
import java.util.function.Supplier;

@Getter
public abstract class AbstractInlineSettingsProvider<T extends AbstractInlineNode> {

    private static final String TAG_FORMAT_STRING = "<!-- %s -->";

    private static final String TAG_END_DELIMITER = "/";

    private final String startTag;

    private final String endTag;

    private Function<String, Node> parseMarkdownFunction;

    public AbstractInlineSettingsProvider() {
        this.startTag = String.format(TAG_FORMAT_STRING, getTagName());
        this.endTag = String.format(TAG_FORMAT_STRING, TAG_END_DELIMITER + getTagName());
        this.parseMarkdownFunction = getParseMarkdownFunction();
    }

    @Autowired
    protected void setParseMarkdownFunction(@Lazy MarkdownService markdownService) {
        parseMarkdownFunction = markdownService::parseMarkdownFile;
    }

    protected abstract String getTagName();

    protected abstract Class<T> getInlineNodeClass();

    protected abstract Supplier<T> getInlineNodeSupplier();

}
