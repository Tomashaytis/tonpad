package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.CustomBlockParserFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

public class AbstractCustomBlockParserFactory<T extends AbstractBlock> implements CustomBlockParserFactory {

    private final AbstractBlockParserFactory<T> blockParserFactory;

    public AbstractCustomBlockParserFactory(
            Supplier<T> blockProvider,
            boolean isContainer,
            boolean canContain,
            String startTag,
            String endTag
    ) {
        this.blockParserFactory = new AbstractBlockParserFactory<>(
                blockProvider,
                isContainer,
                canContain,
                startTag,
                endTag
        );
    }

    @Override
    public @NotNull BlockParserFactory apply(@NotNull DataHolder dataHolder) {
        return blockParserFactory;
    }

    @Override
    public @Nullable Set<Class<?>> getAfterDependents() {
        return null;
    }

    @Override
    public @Nullable Set<Class<?>> getBeforeDependents() {
        return null;
    }

    @Override
    public boolean affectsGlobalScope() {
        return false;
    }

}
