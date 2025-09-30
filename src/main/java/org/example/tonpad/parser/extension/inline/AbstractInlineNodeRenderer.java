package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public abstract class AbstractInlineNodeRenderer<T extends AbstractInlineNode> implements NodeRenderer {

    private final Class<T> blockClass;

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(blockClass, this::render));
        return set;
    }

    protected abstract void render(T node, NodeRendererContext context, HtmlWriter html);

}
