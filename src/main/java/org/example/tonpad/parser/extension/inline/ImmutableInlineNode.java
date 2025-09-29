package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public class ImmutableInlineNode extends Node {

    public ImmutableInlineNode(BasedSequence chars) {
        super(chars);
    }

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return new BasedSequence[]{getChars()};
    }

}
