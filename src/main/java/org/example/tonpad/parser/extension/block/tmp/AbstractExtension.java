package org.example.tonpad.parser.extension.block.tmp;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractExtension<T extends AbstractBlock> implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    protected final SettingsProvider<T> settingsProvider = getSettings();

    protected abstract SettingsProvider<T> getSettings();

    protected abstract void render(T node, NodeRendererContext context, HtmlWriter html);

    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {
    }

    @Override
    public void parserOptions(MutableDataHolder options) {
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customBlockParserFactory(new AbstractCustomBlockParserFactory<>(
                settingsProvider::createBlock,
                settingsProvider.isContainer(),
                settingsProvider.isCanContain(),
                settingsProvider.getStartTag(),
                settingsProvider.getEndTag()
        ));
    }

    @Override
    public void extend(HtmlRenderer.@NotNull Builder htmlRendererBuilder, @NotNull String rendererType) {
        if ("HTML".equals(rendererType)) {
            htmlRendererBuilder.nodeRendererFactory(new AbstractNodeRendererFactory<>(
                    settingsProvider.getBlockClass(),
                    settingsProvider.getStartTag()
            ) {
                @Override
                protected void renderHtml(T node, NodeRendererContext context, HtmlWriter html) {
                    render(node, context, html);
                }
            });
        }
    }

}
