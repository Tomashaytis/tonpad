package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Block;

import java.util.Set;

public abstract class AbstractNodeRenderer <T extends Block> implements NodeRenderer {

    protected final SettingsProvider<T> settingsProvider;

    public AbstractNodeRenderer(SettingsProvider<T> settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        return Set.of(new NodeRenderingHandler<>(settingsProvider.getBlockClass(), this::render));
    }

    protected abstract void render(T node, NodeRendererContext context, HtmlWriter html);

}
