package org.example.tonpad.parser.extension.block.immutable;

import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public class ImmutableBlock extends Block {

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return BasedSequence.EMPTY_SEGMENTS;
    }

}
