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
public class TemplateFieldRecord {

    public TemplateFieldRecord(String field) {
        this.field = field;
    }

    public static final Table<?> TEMPLATE_FIELDS = DSL.table("template_fields");

    public static final Field<Integer> ID = DSL.field("id", Integer.class);
    public static final Field<String> FIELD = DSL.field("field", String.class);

    private Integer id;

    private String field;
}
