package org.example.tonpad.core.models;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateFieldRecord {

    public TemplateFieldRecord(String field) {
        this.field = field;
    }

    private Integer id;

    private int templateId;

    private String field;
}
