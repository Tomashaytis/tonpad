package org.example.tonpad.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRecord {

    public TemplateRecord(String name) {
        this.name = name;
    }

    public static final Table<?> TEMPLATES = DSL.table("templates");

    public static final Field<Integer> ID = DSL.field("id", Integer.class);
    public static final Field<String> NAME = DSL.field("name", String.class);

    private Integer id;

    private String name;
}
