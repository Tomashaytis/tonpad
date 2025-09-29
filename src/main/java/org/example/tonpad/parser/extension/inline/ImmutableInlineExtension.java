package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class ImmutableInlineExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    @Override
    public void rendererOptions(@NotNull MutableDataHolder mutableDataHolder) {
    }

    @Override
    public void parserOptions(MutableDataHolder mutableDataHolder) {
    }

    @Override
    public void extend(HtmlRenderer.@NotNull Builder builder, @NotNull String s) {
        builder.nodeRendererFactory(new ImmutableNodeRendererFactory());
    }

    @Override
    public void extend(Parser.Builder builder) {
        builder.customInlineParserExtensionFactory(new ImmutableInlineParserExtensionFactory());
    }

}
