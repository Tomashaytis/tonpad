package org.example.tonpad.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotesToTemplatesRecord {

    public NotesToTemplatesRecord(int noteId, int templateId) {
        this.noteId = noteId;
        this.templateId = templateId;
    }

    private Integer id;

    private int noteId;

    private int templateId;
}
