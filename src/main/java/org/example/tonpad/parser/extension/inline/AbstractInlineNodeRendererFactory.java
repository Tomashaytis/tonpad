package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractInlineNodeRendererFactory<T extends AbstractInlineNode> implements NodeRendererFactory {

    private final Class<T> inlineNodeClass;

    private final String startTag;

    private final String endTag;

    public AbstractInlineNodeRendererFactory(AbstractInlineSettingsProvider<T> settingsProvider) {
        this.inlineNodeClass = settingsProvider.getInlineNodeClass();
        this.startTag = settingsProvider.getStartTag();
        this.endTag = settingsProvider.getEndTag();
    }

    @Override
    public @NotNull NodeRenderer apply(@NotNull DataHolder options) {
        return new AbstractInlineNodeRenderer<>(inlineNodeClass) {
            @Override
            protected void render(T node, NodeRendererContext context, HtmlWriter html) {
                html.raw(startTag);
                renderHtml(node, context, html);
                html.raw(endTag);
            }
        };
    }

    protected abstract void renderHtml(T node, NodeRendererContext context, HtmlWriter html);

}
