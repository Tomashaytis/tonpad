package org.example.tonpad.core.parser.extension.block.immutable;

import org.example.tonpad.core.parser.extension.block.AbstractBlockSettingsProvider;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ImmutableBlockSettingsProvider extends AbstractBlockSettingsProvider<ImmutableBlock> {

    @Override
    protected String getTagName() {
        return "immutable-block";
    }

    @Override
    protected Class<ImmutableBlock> getBlockClass() {
        return ImmutableBlock.class;
    }

    @Override
    protected Supplier<ImmutableBlock> getBlockSupplier() {
        return ImmutableBlock::new;
    }

}
