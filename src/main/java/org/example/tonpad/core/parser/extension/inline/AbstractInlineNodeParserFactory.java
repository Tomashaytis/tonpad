package org.example.tonpad.core.parser.extension.inline;

import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.util.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractInlineNodeParserFactory<T extends AbstractInlineNode> implements InlineParserExtensionFactory {

    private final Function<String, Node> parseMarkdownFunction;

    private final Supplier<T> inlineNodeSupplier;

    private final String startTag;

    private final String endTag;

    public AbstractInlineNodeParserFactory(AbstractInlineSettingsProvider<T> settingsProvider) {
        parseMarkdownFunction = settingsProvider.getParseMarkdownFunction();
        inlineNodeSupplier = settingsProvider.getInlineNodeSupplier();
        startTag = settingsProvider.getStartTag();
        endTag = settingsProvider.getEndTag();
    }

    @Override
    public @NotNull CharSequence getCharacters() {
        return startTag.substring(0, 1);
    }

    @Override
    public Set<Class<?>> getBeforeDependents() {
        return null;
    }

    @Override
    public Set<Class<?>> getAfterDependents() {
        return null;
    }

    @Override
    public @NotNull InlineParserExtension apply(@NotNull LightInlineParser lightInlineParser) {
        return new InlineNodeParser<>(parseMarkdownFunction, inlineNodeSupplier, startTag, endTag);
    }

    @Override
    public boolean affectsGlobalScope() {
        return false;
    }

}
