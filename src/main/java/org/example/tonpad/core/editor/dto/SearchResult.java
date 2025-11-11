package org.example.tonpad.core.editor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult {

    private boolean isActive;

    private String query;

    private int current;

    private int total;

    private boolean hasResults;
}
