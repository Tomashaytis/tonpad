package org.example.tonpad.core.parser.extension.block.immutable;

import org.example.tonpad.core.parser.extension.block.AbstractCustomBlockParserFactory;
import org.example.tonpad.core.parser.extension.block.AbstractBlockSettingsProvider;
import org.springframework.stereotype.Component;

@Component
public class ImmutableCustomBlockParserFactory extends AbstractCustomBlockParserFactory<ImmutableBlock> {

    protected ImmutableCustomBlockParserFactory(AbstractBlockSettingsProvider<ImmutableBlock> blockSettingsProvider) {
        super(blockSettingsProvider);
    }

}
