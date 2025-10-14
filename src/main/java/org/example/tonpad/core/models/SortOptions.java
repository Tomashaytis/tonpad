package org.example.tonpad.core.models;

public record SortOptions(SortKey key, boolean foldersFirst, boolean relevantOnly) {
    public static SortOptions defaults()
    {
        return new SortOptions(SortKey.NAME_ASC, true, false);
    }
}   
