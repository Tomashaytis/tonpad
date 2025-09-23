package org.example.tonpad.parser.extension.block;

import com.vladsch.flexmark.util.ast.Block;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class SettingsProvider<T extends Block> {

    private static final String TAG_FORMAT_STRING = "<!-- %s -->";

    private static final String TAG_END_DELIMETER = "/";

    private final String startTag;

    private final String endTag;

    private final Class<T> blockClass;

    private final boolean isContainer;

    private final boolean canContain;

    public SettingsProvider(String tagName,Class<T> blockClass, boolean isContainer, boolean canContain) {
        this.startTag = String.format(TAG_FORMAT_STRING, tagName);
        this.endTag = String.format(TAG_FORMAT_STRING, TAG_END_DELIMETER + tagName);
        this.blockClass = blockClass;
        this.isContainer = isContainer;
        this.canContain = canContain;
    }

    @SneakyThrows
    public T createBlock() {
        return blockClass.getDeclaredConstructor().newInstance();
    }

    public static class SettingsProviderBuilder<T extends Block> {

        private String tagName;

        private final Class<T> blockClass;

        private boolean isContainer = true;

        private boolean canContain = true;

        public SettingsProviderBuilder(Class<T> blockClass) {
            this.blockClass = blockClass;
        }

        public SettingsProviderBuilder<T> tagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        public SettingsProviderBuilder<T> container(boolean container) {
            isContainer = container;
            return this;
        }

        public SettingsProviderBuilder<T> canContain(boolean canContain) {
            this.canContain = canContain;
            return this;
        }

        public SettingsProvider<T> build() {
            return new SettingsProvider<>(tagName, blockClass, isContainer, canContain);
        }
    }
}
