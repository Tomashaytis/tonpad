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
public class NotesDataRecord {

    public NotesDataRecord(String notePath, Integer templateId, String field, String value) {
        this.notePath = notePath;
        this.templateId = templateId;
        this.field = field;
        this.value = value;
    }

    public static final Table<?> NOTES_DATA = DSL.table("notes_data");

    public static final Field<Integer> ID = DSL.field("id", Integer.class);
    public static final Field<String> NOTE_PATH = DSL.field("note_path", String.class);
    public static final Field<Integer> TEMPLATE_ID = DSL.field("template_id", Integer.class);
    public static final Field<String> FIELD = DSL.field("field", String.class);
    public static final Field<String> VALUE = DSL.field("value", String.class);

    private Integer id;

    private String notePath;

    private Integer templateId;

    private String field;

    private String value;
}
