package org.example.tonpad.core.parser.extension.block.immutable;

import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.example.tonpad.core.parser.extension.block.AbstractBlock;
import org.jetbrains.annotations.NotNull;

public class ImmutableBlock extends AbstractBlock {

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return BasedSequence.EMPTY_SEGMENTS;
    }

}
