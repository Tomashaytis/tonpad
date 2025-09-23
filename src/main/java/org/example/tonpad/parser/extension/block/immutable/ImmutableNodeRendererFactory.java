package org.example.tonpad.parser.extension.block.immutable;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import org.example.tonpad.parser.extension.block.AbstractNodeRendererFactory;
import org.example.tonpad.parser.extension.block.Settings;

public class ImmutableNodeRendererFactory extends AbstractNodeRendererFactory<ImmutableBlock> {

    public ImmutableNodeRendererFactory(Settings<ImmutableBlock> settings) {
        super(settings);
    }

    @Override
    protected void renderHtml(ImmutableBlock node, NodeRendererContext context, HtmlWriter html) {
        html.attr("contenteditable", "false");
        html.withAttr().tag("div");
        context.renderChildren(node);
        html.tag("/div");
    }

}
