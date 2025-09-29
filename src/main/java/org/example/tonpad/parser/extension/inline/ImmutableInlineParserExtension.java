package org.example.tonpad.parser.extension.inline;

import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImmutableInlineParserExtension implements InlineParserExtension {

    private static final Pattern IMMUTABLE_PATTERN = Pattern.compile(
            "<!--\\s*immutable-inline\\s*-->(.*?)<!--\\s*/immutable-inline\\s*-->",
            Pattern.DOTALL
    );

    @Override
    public void finalizeDocument(InlineParser inlineParser) {
    }

    @Override
    public void finalizeBlock(InlineParser inlineParser) {
    }

    @Override
    public boolean parse(LightInlineParser inlineParser) {
        BasedSequence input = inlineParser.getInput();
        int index = inlineParser.getIndex();

        // Проверяем начало комментария
        if (!input.matchChars("<!--", index)) {
            return false;
        }

        BasedSequence remaining = input.subSequence(index, input.length());
        Matcher matcher = IMMUTABLE_PATTERN.matcher(remaining);

        if (!matcher.lookingAt()) {
            return false;
        }

        // полный матч и только содержимое между тегами
        BasedSequence matchedText = remaining.subSequence(0, matcher.end());
        BasedSequence contentSeq = remaining.subSequence(matcher.start(1), matcher.end(1));

        // Создаём контейнерный узел (подразумеваем, что ImmutableInlineNode — контейнер)
        ImmutableInlineNode node = new ImmutableInlineNode(contentSeq);

        // Помещаем наш узел в AST на текущую позицию
        inlineParser.flushTextNode();
        inlineParser.getBlock().appendChild(node);

        // Парсим содержимое отдельно (обратный парсер Markdown)
        Parser innerParser = Parser.builder().build();
        Node innerDoc = innerParser.parse(contentSeq.toString());

        // Проходим по верхнеуровневым блокам результата парсинга
        for (Node block = innerDoc.getFirstChild(); block != null; block = block.getNext()) {
            if (block instanceof Paragraph) {
                // Переносим inline-детей параграфа в наш inline-контейнер
                Node inlineChild = block.getFirstChild();
                while (inlineChild != null) {
                    Node nextInline = inlineChild.getNext(); // запомним следующий, т.к. unlink() ломает связи
                    inlineChild.unlink();                    // удаляем из старого места
                    node.appendChild(inlineChild);           // добавляем в наш inline-контейнер
                    inlineChild = nextInline;
                }
            } else {
                // Если встретился какой-то блочный элемент (heading, list и т.п.) —
                // можно либо проигнорировать, либо добавить его текстовое представление.
                // Здесь добавим простой Text с literal'ом блока:
                BasedSequence blockChars = block.getChars();
                if (!blockChars.isEmpty()) {
                    node.appendChild(new com.vladsch.flexmark.ast.Text(blockChars));
                }
            }

            // Если после этого блока ещё есть блоки — вставим разделитель (soft line break)
            if (block.getNext() != null) {
                node.appendChild(new com.vladsch.flexmark.ast.SoftLineBreak());
            }
        }

        // Продвигаем индекс входа
        inlineParser.setIndex(index + matcher.end());
        return true;
    }

}
