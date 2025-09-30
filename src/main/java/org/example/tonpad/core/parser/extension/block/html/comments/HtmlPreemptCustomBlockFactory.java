package org.example.tonpad.core.parser.extension.block.html.comments;

import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.CustomBlockParserFactory;
import com.vladsch.flexmark.parser.core.HtmlBlockParser;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public class HtmlPreemptCustomBlockFactory implements CustomBlockParserFactory {

    @Override
    public @NotNull BlockParserFactory apply(@NotNull DataHolder dataHolder) {
        return new HtmlPreemptBlockFactory();
    }

    @Override
    public @Nullable Set<Class<?>> getAfterDependents() {
        return Set.of();
    }

    @Override
    public @Nullable Set<Class<?>> getBeforeDependents() {
        return Collections.singleton(HtmlBlockParser.Factory.class);
    }

    @Override
    public boolean affectsGlobalScope() {
        return false;
    }

}
