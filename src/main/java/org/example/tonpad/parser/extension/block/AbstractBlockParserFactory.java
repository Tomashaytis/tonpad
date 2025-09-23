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

    private final SettingsProvider<T> settingsProvider;

    public AbstractBlockParserFactory(SettingsProvider<T> settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    @Override
    public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
        BasedSequence line = state.getLine();
        if (line.toString().trim().equals(settingsProvider.getStartTag())) {
            AbstractBlockParser blockParser = new AbstractBlockParser(settingsProvider.createBlock(), settingsProvider.isContainer(), settingsProvider.isCanContain());
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
            if (line.toString().trim().equals(settingsProvider.getEndTag())) {
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
