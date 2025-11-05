package org.example.tonpad.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRecord {

    public TemplateRecord(String name) {
        this.name = name;
    }

    private Integer id;

    private String name;

    private List<String> fields;
}
