package org.example.tonpad.parser.extension.inline.immutable;

import org.example.tonpad.parser.extension.inline.AbstractInlineExtension;
import org.example.tonpad.parser.extension.inline.AbstractInlineNodeParserFactory;
import org.example.tonpad.parser.extension.inline.AbstractInlineNodeRendererFactory;
import org.springframework.stereotype.Component;

@Component
public class ImmutableInlineExtension extends AbstractInlineExtension<ImmutableInlineNode> {

    public ImmutableInlineExtension(AbstractInlineNodeParserFactory<ImmutableInlineNode> customBlockParserFactory, AbstractInlineNodeRendererFactory<ImmutableInlineNode> nodeRendererFactory) {
        super(customBlockParserFactory, nodeRendererFactory);
    }

}
