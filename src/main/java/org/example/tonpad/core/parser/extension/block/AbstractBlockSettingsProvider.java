package org.example.tonpad.parser.extension.block;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public abstract class AbstractBlockSettingsProvider<T extends AbstractBlock> {

    private static final String TAG_FORMAT_STRING = "<!-- %s -->";

    private static final String TAG_END_DELIMITER = "/";

    private final String startTag;

    private final String endTag;

    protected AbstractBlockSettingsProvider() {
        this.startTag = String.format(TAG_FORMAT_STRING, getTagName());
        this.endTag = String.format(TAG_FORMAT_STRING, TAG_END_DELIMITER + getTagName());
    }

    protected abstract String getTagName();

    protected abstract Class<T> getBlockClass();

    protected abstract Supplier<T> getBlockSupplier();

}
