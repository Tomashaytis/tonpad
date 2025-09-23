package org.example.tonpad.parser.extension.block.immutable;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import org.example.tonpad.parser.extension.block.tmp.AbstractExtension;
import org.example.tonpad.parser.extension.block.tmp.SettingsProvider;
import org.springframework.stereotype.Component;

@Component
public class ImmutableExtension extends AbstractExtension<ImmutableBlock> {

    @Override
    protected SettingsProvider<ImmutableBlock> getSettings() {
        return new SettingsProvider.SettingsProviderBuilder<>("immutable", ImmutableBlock.class)
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
