package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor
abstract class AbstractNodeRenderer<T extends AbstractBlock> implements NodeRenderer {

    private final Class<T> blockClass;

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        return Set.of(new NodeRenderingHandler<>(blockClass, this::render));
    }

    protected abstract void render(T node, NodeRendererContext context, HtmlWriter html);

}
