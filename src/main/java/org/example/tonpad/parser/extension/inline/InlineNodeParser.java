package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.example.tonpad.core.exceptions.BlockInsideInlineTagException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineNodeParser<T extends AbstractInlineNode> implements InlineParserExtension {

    private final Function<String, Node> parseMarkdownFunction;

    private final Supplier<T> inlineNodeSupplier;

    private final Pattern textWithInlineTagsPattern;

    private final String startTag;

    public InlineNodeParser(
            Function<String, Node> parseMarkdownFunction,
            Supplier<T> inlineNodeSupplier,
            String startTag,
            String endTag
    ) {
        this.parseMarkdownFunction = parseMarkdownFunction;
        this.inlineNodeSupplier = inlineNodeSupplier;
        textWithInlineTagsPattern = Pattern.compile(
                String.format("%s(.*?)%s", startTag, endTag),
                Pattern.DOTALL
        );
        this.startTag = startTag;
    }

    @Override
    public void finalizeDocument(@NotNull InlineParser inlineParser) {
    }

    @Override
    public void finalizeBlock(@NotNull InlineParser inlineParser) {
    }

    @Override
    public boolean parse(LightInlineParser inlineParser) {
        BasedSequence input = inlineParser.getInput();
        int index = inlineParser.getIndex();

        if (!input.matchChars(startTag, index)) {
            return false;
        }

        BasedSequence remaining = input.subSequence(index, input.length());
        Matcher matcher = textWithInlineTagsPattern.matcher(remaining);

        if (!matcher.lookingAt()) {
            return false;
        }

        BasedSequence content = remaining.subSequence(matcher.start(1), matcher.end(1));
        T node = inlineNodeSupplier.get();

        inlineParser.flushTextNode();
        inlineParser.getBlock().appendChild(node);

        Node innerDoc = parseMarkdownFunction.apply(content.toString());
        for (Node block = innerDoc.getFirstChild(); block != null; block = block.getNext()) {
            if (!(block instanceof Paragraph)) {
                throw new BlockInsideInlineTagException("внутри инлайн тэга есть блок, что-то пошло не так");
            }

            Node child = block.getFirstChild();
            while (child != null) {
                Node nextInline = child.getNext();

                child.unlink();
                node.appendChild(child);

                child = nextInline;
            }

            if (block.getNext() != null) {
                node.appendChild(new com.vladsch.flexmark.ast.SoftLineBreak());
            }
        }

        inlineParser.setIndex(index + matcher.end());
        return true;
    }

}
