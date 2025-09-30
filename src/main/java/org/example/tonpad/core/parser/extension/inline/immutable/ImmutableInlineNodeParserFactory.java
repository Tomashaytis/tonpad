package org.example.tonpad.core.parser.extension.inline.immutable;

import org.example.tonpad.core.parser.extension.inline.AbstractInlineNodeParserFactory;
import org.example.tonpad.core.parser.extension.inline.AbstractInlineSettingsProvider;
import org.springframework.stereotype.Component;

@Component
public class ImmutableInlineNodeParserFactory extends AbstractInlineNodeParserFactory<ImmutableInlineNode> {

    public ImmutableInlineNodeParserFactory(AbstractInlineSettingsProvider<ImmutableInlineNode> settingsProvider) {
        super(settingsProvider);
    }

}
