package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.parser.block.BlockStart;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class BlockParserFactory<T extends AbstractBlock> implements com.vladsch.flexmark.parser.block.BlockParserFactory {

    private final String startTag;

    private final String endTag;

    private final Supplier<T> blockSupplier;

    @Override
    public BlockStart tryStart(ParserState parserState, MatchedBlockParser matchedBlockParser) {
        BasedSequence line = parserState.getLine();
        if (line.toString().trim().equals(startTag)) {
            BlockParser blockParser = new BlockParser(blockSupplier.get(), endTag);
            return BlockStart.of(blockParser).atIndex(line.length());
        }

        return BlockStart.none();
    }

}
