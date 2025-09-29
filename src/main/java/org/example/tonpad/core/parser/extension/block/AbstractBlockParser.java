package org.example.tonpad.core.parser.extension.block;

import com.vladsch.flexmark.parser.block.BlockContinue;
import com.vladsch.flexmark.parser.block.BlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class AbstractBlockParser<T extends AbstractBlock> extends com.vladsch.flexmark.parser.block.AbstractBlockParser {

    private final T block;

    private final boolean isContainer;

    private final boolean canContain;

    private final String endTag;

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        BasedSequence line = state.getLine();
        if (line.toString().trim().equals(endTag)) {
            return BlockContinue.none();
        }

        return BlockContinue.atIndex(state.getIndex());
    }

    @Override
    public void closeBlock(ParserState state) {
        block.setCharsFromContent();
    }

    @Override
    public boolean isContainer() {
        return isContainer;
    }

    @Override
    public boolean canContain(ParserState state, BlockParser blockParser, Block block) {
        return canContain;
    }

}
