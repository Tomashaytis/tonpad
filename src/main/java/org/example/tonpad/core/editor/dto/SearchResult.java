package org.example.tonpad.core.editor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult {

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("query")
    private String query;

    @JsonProperty("current")
    private int current;

    @JsonProperty("total")
    private int total;

    @JsonProperty("hasResults")
    private boolean hasResults;
}
