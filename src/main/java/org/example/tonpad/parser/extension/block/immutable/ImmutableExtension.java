package org.example.tonpad.parser.extension.block.immutable;

import org.example.tonpad.parser.extension.block.AbstractBlockParserFactory;
import org.example.tonpad.parser.extension.block.AbstractCustomBlockParserFactory;
import org.example.tonpad.parser.extension.block.AbstractExtension;
import org.example.tonpad.parser.extension.block.AbstractNodeRendererFactory;
import org.example.tonpad.parser.extension.block.Settings;

public class ImmutableExtension extends AbstractExtension<ImmutableBlock> {

    Settings<ImmutableBlock> settings = new Settings<>(
            "immutable start",
            "immutable end",
            new ImmutableBlock(),
            ImmutableBlock.class,
            true,
            true
    );

    @Override
    protected AbstractCustomBlockParserFactory<ImmutableBlock> getCustomBlockParserFactory() {
        return new AbstractCustomBlockParserFactory<>(new AbstractBlockParserFactory<>(settings));
    }

    @Override
    protected AbstractNodeRendererFactory<ImmutableBlock> getNodeRendererFactory() {
        return new ImmutableNodeRendererFactory(settings);
    }

}
