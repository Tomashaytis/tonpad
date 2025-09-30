package org.example.tonpad.parser.extension.block.immutable;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import org.example.tonpad.parser.extension.block.AbstractBlockSettingsProvider;
import org.example.tonpad.parser.extension.block.AbstractNodeRendererFactory;
import org.springframework.stereotype.Component;

@Component
public class ImmutableNodeRendererFactory extends AbstractNodeRendererFactory<ImmutableBlock> {

    public ImmutableNodeRendererFactory(AbstractBlockSettingsProvider<ImmutableBlock> blockSettingsProvider) {
        super(blockSettingsProvider);
    }

    @Override
    protected void renderHtml(ImmutableBlock node, NodeRendererContext context, HtmlWriter html) {
        html.attr("contenteditable", "false");
        html.withAttr().tag("div");
        context.renderChildren(node);
        html.tag("/div");
    }

}
