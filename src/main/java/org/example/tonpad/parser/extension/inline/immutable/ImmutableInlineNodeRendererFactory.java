package org.example.tonpad.parser.extension.inline.immutable;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import org.example.tonpad.parser.extension.inline.AbstractInlineNodeRendererFactory;
import org.example.tonpad.parser.extension.inline.AbstractInlineSettingsProvider;
import org.springframework.stereotype.Component;

@Component
public class ImmutableInlineNodeRendererFactory extends AbstractInlineNodeRendererFactory<ImmutableInlineNode> {

    public ImmutableInlineNodeRendererFactory(AbstractInlineSettingsProvider<ImmutableInlineNode> settingsProvider) {
        super(settingsProvider);
    }

    @Override
    protected void renderHtml(ImmutableInlineNode node, NodeRendererContext context, HtmlWriter html) {
        html.attr("contenteditable", "false");
        //html.attr("data-immutable", "true");
        html.withAttr().tag("span");
        context.renderChildren(node);
        html.tag("/span");
    }

}
