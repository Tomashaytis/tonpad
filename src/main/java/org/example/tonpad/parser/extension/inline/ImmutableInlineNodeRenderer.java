package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;

import java.util.HashSet;
import java.util.Set;

public class ImmutableInlineNodeRenderer implements NodeRenderer {

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(ImmutableInlineNode.class, this::render));
        return set;
    }

    private void render(ImmutableInlineNode node, NodeRendererContext context, HtmlWriter html) {
        html.attr("contenteditable", "false");
        html.attr("data-immutable", "true");
        html.withAttr().tag("span");
        context.renderChildren(node);
        html.tag("/span");
    }

}
