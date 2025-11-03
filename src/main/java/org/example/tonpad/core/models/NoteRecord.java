package org.example.tonpad.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteRecord {

    public NoteRecord(String name) {
        this.name = name;
    }

    private Integer id;

    private String name;
}
