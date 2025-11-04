package org.example.tonpad.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotesAssociationsRecord {

    public NotesAssociationsRecord(int srcId, int dstId) {
        this.srcId = srcId;
        this.dstId = dstId;
    }

    private Integer id;

    private int srcId;

    private int dstId;
}
