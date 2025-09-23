package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNodeRendererFactory <T extends Block> implements NodeRendererFactory {

    private final Settings<T> settings;

    public AbstractNodeRendererFactory(Settings<T> settings) {
        this.settings = settings;
    }

    @Override
    public @NotNull NodeRenderer apply(@NotNull DataHolder dataHolder) {
        return new AbstractNodeRenderer<>(settings) {
            @Override
            protected void render(T node, NodeRendererContext context, HtmlWriter html) {
                html.raw( settings.getStartTag() + "\n");
                renderHtml(node, context, html);
            }
        };
    }

    protected abstract void renderHtml(T node, NodeRendererContext context, HtmlWriter html);

}
