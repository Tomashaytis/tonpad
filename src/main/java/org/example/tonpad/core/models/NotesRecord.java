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
public class NotesRecord {

    /**
     * Поля для использования в запросах
     */
    public static final Table<?> NOTES = DSL.table("notes");

    public static final Field<Integer> ID = DSL.field("id", Integer.class);
    public static final Field<String> NAME = DSL.field("name", String.class);
    public static final Field<String> PATH = DSL.field("path", String.class);

    /**
     * Поля, содержащие результаты запросов
     */
    private Integer id;

    private String name;

    private String path;

}
