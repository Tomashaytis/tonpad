package org.example.tonpad.parser.extension.block.immutable;

import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.example.tonpad.parser.extension.block.tmp.AbstractBlock;
import org.jetbrains.annotations.NotNull;

public class ImmutableBlock extends AbstractBlock {

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return BasedSequence.EMPTY_SEGMENTS;
    }

}
