package org.example.tonpad.core.parser.extension.block;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNodeRendererFactory<T extends AbstractBlock> implements NodeRendererFactory {

    private final Class<T> blockClass;

    private final String startTag;

    public AbstractNodeRendererFactory(AbstractBlockSettingsProvider<T> blockSettingsProvider) {
        blockClass = blockSettingsProvider.getBlockClass();
        startTag = blockSettingsProvider.getStartTag();
    }

    @Override
    public @NotNull NodeRenderer apply(@NotNull DataHolder dataHolder) {
        return new AbstractNodeRenderer<>(blockClass) {
            @Override
            protected void render(T node, NodeRendererContext context, HtmlWriter html) {
                html.raw( startTag + "\n");
                renderHtml(node, context, html);
            }
        };
    }

    protected abstract void renderHtml(T node, NodeRendererContext context, HtmlWriter html);

}
