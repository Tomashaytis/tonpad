package org.example.tonpad.core.parser.extension.block;

import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockStart;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import lombok.AllArgsConstructor;

import java.util.function.Supplier;

@AllArgsConstructor
class AbstractBlockParserFactory<T extends AbstractBlock> implements BlockParserFactory {

    private final Supplier<T> blockProvider;

    private final boolean isContainer;

    private final boolean canContain;

    private final String startTag;

    private final String endTag;

    @Override
    public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
        BasedSequence line = state.getLine();
        if (line.toString().trim().equals(startTag)) {
            AbstractBlockParser<T> blockParser = new AbstractBlockParser<>(
                    blockProvider.get(),
                    isContainer,
                    canContain,
                    endTag
            );
            return BlockStart.of(blockParser).atIndex(line.length());
        }

        return BlockStart.none();
    }

}
