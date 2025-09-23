package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.util.ast.Block;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class SettingsProvider<T extends Block> {

    private static final String TAG_FORMAT_STRING = "<!-- %s -->";

    private final String startTag;

    private final String endTag;

    private final Class<T> blockClass;

    private final boolean isContainer;

    private final boolean canContain;

    public SettingsProvider(String startTagName, String endTagName, Class<T> blockClass, boolean isContainer, boolean canContain) {
        this.startTag = String.format(TAG_FORMAT_STRING, startTagName);
        this.endTag = String.format(TAG_FORMAT_STRING, endTagName);
        this.blockClass = blockClass;
        this.isContainer = isContainer;
        this.canContain = canContain;
    }

    @SneakyThrows
    public T createBlock() {
        return blockClass.getDeclaredConstructor().newInstance();
    }

}
