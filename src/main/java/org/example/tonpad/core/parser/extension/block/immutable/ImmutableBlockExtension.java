package org.example.tonpad.core.parser.extension.block.immutable;

import org.example.tonpad.core.parser.extension.block.AbstractBlockExtension;
import org.example.tonpad.core.parser.extension.block.AbstractCustomBlockParserFactory;
import org.example.tonpad.core.parser.extension.block.AbstractNodeRendererFactory;
import org.springframework.stereotype.Component;

@Component
public class ImmutableBlockExtension extends AbstractBlockExtension<ImmutableBlock> {

    public ImmutableBlockExtension(AbstractCustomBlockParserFactory<ImmutableBlock> customBlockParserFactory, AbstractNodeRendererFactory<ImmutableBlock> nodeRendererFactory) {
        super(customBlockParserFactory, nodeRendererFactory);
    }

}
