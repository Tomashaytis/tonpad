package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ImmutableInlineParserExtensionFactory implements InlineParserExtensionFactory {

    @Override
    public Set<Class<?>> getAfterDependents() {
        return null;
    }

    @Override
    public @NotNull CharSequence getCharacters() {
        return "<";
    }

    @Override
    public Set<Class<?>> getBeforeDependents() {
        return null;
    }

    @Override
    public @NotNull InlineParserExtension apply(@NotNull LightInlineParser lightInlineParser) {
        return new ImmutableInlineParserExtension();
    }

    @Override
    public boolean affectsGlobalScope() {
        return false;
    }

}
