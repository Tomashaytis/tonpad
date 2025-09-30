package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class AbstractBlockExtension<T extends AbstractBlock> implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    private final AbstractCustomBlockParserFactory<T> customBlockParserFactory;

    private final AbstractNodeRendererFactory<T> nodeRendererFactory;

    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {
    }

    @Override
    public void parserOptions(MutableDataHolder options) {
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customBlockParserFactory(customBlockParserFactory);
    }

    @Override
    public void extend(HtmlRenderer.@NotNull Builder htmlRendererBuilder, @NotNull String rendererType) {
        htmlRendererBuilder.nodeRendererFactory(nodeRendererFactory);
    }

}
