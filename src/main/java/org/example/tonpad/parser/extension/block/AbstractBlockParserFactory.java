package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.parser.block.BlockContinue;
import com.vladsch.flexmark.parser.block.BlockParser;
import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockStart;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.sequence.BasedSequence;

public class AbstractBlockParserFactory <T extends Block> implements BlockParserFactory {

    private final AbstractBlockParser blockParser;

    private final String startTag;

    private final String endTag;

    public AbstractBlockParserFactory(Settings<T> settings) {
        blockParser = new AbstractBlockParser(settings.getBlock(), settings.isContainer(), settings.isCanContain());
        this.startTag = settings.getStartTag();
        this.endTag = settings.getEndTag();
    }

    @Override
    public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
        BasedSequence line = state.getLine();
        if (line.toString().trim().equals(startTag)) {
            return BlockStart.of(blockParser).atIndex(line.length());
        }

        return BlockStart.none();
    }

    private class AbstractBlockParser extends com.vladsch.flexmark.parser.block.AbstractBlockParser {

        private final T block;

        private final boolean isContainer;

        private final boolean canContain;

        public AbstractBlockParser(T block, boolean isContainer, boolean canContain) {
            this.block = block;
            this.isContainer = isContainer;
            this.canContain = canContain;
        }

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

}
