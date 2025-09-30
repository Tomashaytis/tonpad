package org.example.tonpad.core.parser.extension.block.html.comments;

import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockStart;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.parser.core.ParagraphParser;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.regex.Pattern;

public class HtmlPreemptBlockFactory implements BlockParserFactory {

    private static final Pattern INLINE_TAG_PATTERN = Pattern.compile("<!-- [^-]*-inline -->");

    @Override
    public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
        if (state.getIndent() >= 4) {
            return BlockStart.none();
        }

        BasedSequence line = state.getLine().subSequence(state.getIndex());

        /*line.startsWith("<!-- immutable-inline -->")*/
        if (INLINE_TAG_PATTERN.matcher(line.toString()).lookingAt()) {
            return BlockStart.of(new ParagraphParser());
        }

        return BlockStart.none();
    }

}
