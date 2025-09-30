package org.example.tonpad.core.parser.extension.inline.immutable;

import org.example.tonpad.core.parser.extension.inline.AbstractInlineSettingsProvider;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ImmutableInlineSettingsProvider extends AbstractInlineSettingsProvider<ImmutableInlineNode> {

    @Override
    protected String getTagName() {
        return "immutable-inline";
    }

    @Override
    protected Class<ImmutableInlineNode> getInlineNodeClass() {
        return ImmutableInlineNode.class;
    }

    @Override
    protected Supplier<ImmutableInlineNode> getInlineNodeSupplier() {
        return ImmutableInlineNode::new;
    }

}
