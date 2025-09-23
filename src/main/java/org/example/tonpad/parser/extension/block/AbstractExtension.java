package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractExtension <T extends Block> implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    protected abstract AbstractCustomBlockParserFactory<T> getCustomBlockParserFactory();

    protected abstract AbstractNodeRendererFactory<T> getNodeRendererFactory();

    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {
    }

    @Override
    public void parserOptions(MutableDataHolder options) {
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customBlockParserFactory(getCustomBlockParserFactory());
    }

    @Override
    public void extend(HtmlRenderer.@NotNull Builder htmlRendererBuilder, @NotNull String rendererType) {
        if ("HTML".equals(rendererType)) {
            htmlRendererBuilder.nodeRendererFactory(getNodeRendererFactory());
        }
    }

}
