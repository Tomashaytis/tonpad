package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.util.ast.Block;
import lombok.Getter;

@Getter
public class Settings<T extends Block> {

    private static final String TAG_FORMAT_STRING = "<!-- %s -->";

    private final String startTag;

    private final String endTag;

    private final T block;

    private final Class<T> blockClass;

    private final boolean isContainer;

    private final boolean canContain;

    public Settings(String startTagName, String endTagName, T block, Class<T> blockClass, boolean isContainer, boolean canContain) {
        this.startTag = String.format(TAG_FORMAT_STRING, startTagName);
        this.endTag = String.format(TAG_FORMAT_STRING, endTagName);
        this.block = block;
        this.blockClass = blockClass;
        this.isContainer = isContainer;
        this.canContain = canContain;
    }

}
