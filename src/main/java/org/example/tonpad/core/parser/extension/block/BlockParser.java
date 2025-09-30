package org.example.tonpad.core.parser.extension.block;

import com.vladsch.flexmark.parser.block.BlockContinue;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlockParser extends com.vladsch.flexmark.parser.block.AbstractBlockParser {

    private final AbstractBlock block;

    private final String endTag;

    @Override
    public com.vladsch.flexmark.util.ast.Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState parserState) {
        BasedSequence line = parserState.getLine();
        if (line.toString().trim().equals(endTag)) {
            return BlockContinue.none();
        }

        return BlockContinue.atIndex(parserState.getIndex());
    }

    @Override
    public void closeBlock(ParserState parserState) {
        block.setCharsFromContent();
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public boolean canContain(ParserState state, com.vladsch.flexmark.parser.block.BlockParser blockParser, com.vladsch.flexmark.util.ast.Block block) {
        return true;
    }

}
