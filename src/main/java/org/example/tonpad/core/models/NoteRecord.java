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

    public NoteRecord(String path) {
        this.path = path;
    }

    private Integer id;

    private String path;
}
