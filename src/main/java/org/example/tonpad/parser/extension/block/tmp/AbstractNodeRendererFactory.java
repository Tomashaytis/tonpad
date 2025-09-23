package org.example.tonpad.parser.extension.block.tmp;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public abstract class AbstractNodeRendererFactory<T extends AbstractBlock> implements NodeRendererFactory {

    private final Class<T> blockClass;

    private final String startTag;

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
