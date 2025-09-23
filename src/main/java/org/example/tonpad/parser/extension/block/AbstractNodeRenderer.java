package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Block;

import java.util.Set;

public abstract class AbstractNodeRenderer <T extends Block> implements NodeRenderer {

    protected final Settings<T> settings;

    public AbstractNodeRenderer(Settings<T> settings) {
        this.settings = settings;
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        return Set.of(new NodeRenderingHandler<>(settings.getBlockClass(), this::render));
    }

    protected abstract void render(T node, NodeRendererContext context, HtmlWriter html);

}
