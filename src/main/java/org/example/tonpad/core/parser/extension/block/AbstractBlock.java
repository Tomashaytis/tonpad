package org.example.tonpad.core.parser.extension.block;

import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBlock extends Block {

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return BasedSequence.EMPTY_SEGMENTS;
    }

}
