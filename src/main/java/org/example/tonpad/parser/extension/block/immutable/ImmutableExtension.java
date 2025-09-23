package org.example.tonpad.parser.extension.block.immutable;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import org.example.tonpad.parser.extension.block.AbstractExtension;
import org.example.tonpad.parser.extension.block.SettingsProvider;

public class ImmutableExtension extends AbstractExtension<ImmutableBlock> {

    @Override
    protected SettingsProvider<ImmutableBlock> getSettings() {
        return new SettingsProvider.SettingsProviderBuilder<>(ImmutableBlock.class)
                .tagName("immutable")
                .build();
    }

    @Override
    protected void render(ImmutableBlock node, NodeRendererContext context, HtmlWriter html) {
        html.attr("contenteditable", "false");
        html.withAttr().tag("div");
        context.renderChildren(node);
        html.tag("/div");
    }

}
