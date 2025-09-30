package org.example.tonpad.core.parser.extension.inline;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class AbstractInlineExtension<T extends AbstractInlineNode> implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    private final AbstractInlineNodeParserFactory<T> customBlockParserFactory;

    private final AbstractInlineNodeRendererFactory<T> nodeRendererFactory;

    @Override
    public void rendererOptions(@NotNull MutableDataHolder mutableDataHolder) {
    }

    @Override
    public void parserOptions(MutableDataHolder mutableDataHolder) {
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customInlineParserExtensionFactory(customBlockParserFactory);
    }

    @Override
    public void extend(HtmlRenderer.@NotNull Builder htmlRendererBuilder, @NotNull String rendererType) {
        htmlRendererBuilder.nodeRendererFactory(nodeRendererFactory);
    }

}
