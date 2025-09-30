package org.example.tonpad.core.parser.extension.block.html.comments;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.springframework.stereotype.Component;

@Component
public class HtmlPreemptExtension implements Parser.ParserExtension {

    @Override
    public void parserOptions(MutableDataHolder mutableDataHolder) {}

    @Override
    public void extend(Parser.Builder builder) {
        builder.customBlockParserFactory(new HtmlPreemptCustomBlockFactory());
    }

}
