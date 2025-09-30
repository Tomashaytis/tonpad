package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

public abstract class AbstractCustomBlockParserFactory<T extends AbstractBlock> implements com.vladsch.flexmark.parser.block.CustomBlockParserFactory {

    private final String startTag;

    private final String endTag;

    private final Supplier<T> blockSupplier;

    protected AbstractCustomBlockParserFactory(AbstractBlockSettingsProvider<T> blockSettingsProvider) {
        startTag = blockSettingsProvider.getStartTag();
        endTag = blockSettingsProvider.getEndTag();
        blockSupplier = blockSettingsProvider.getBlockSupplier();
    }

    @Override
    public @NotNull BlockParserFactory apply(@NotNull DataHolder dataHolder) {
        return new org.example.tonpad.parser.extension.block.BlockParserFactory<>(startTag, endTag, blockSupplier);
    }

    @Override
    public @Nullable Set<Class<?>> getAfterDependents() {
        return Set.of();
    }

    @Override
    public @Nullable Set<Class<?>> getBeforeDependents() {
        return Set.of();
    }

    @Override
    public boolean affectsGlobalScope() {
        return false;
    }

}
