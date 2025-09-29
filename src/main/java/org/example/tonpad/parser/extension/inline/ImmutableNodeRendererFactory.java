package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;

public class ImmutableNodeRendererFactory implements NodeRendererFactory {

    @Override
    public @NotNull NodeRenderer apply(@NotNull DataHolder options) {
        return new ImmutableInlineNodeRenderer();
    }

}
