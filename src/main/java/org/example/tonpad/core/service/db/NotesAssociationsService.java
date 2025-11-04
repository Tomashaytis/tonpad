package org.example.tonpad.core.service.db;

import org.example.tonpad.core.models.NotesAssociationsRecord;

import java.util.List;

public interface NotesAssociationsService {

    List<NotesAssociationsRecord> getAll();

    NotesAssociationsRecord getById(int id);

    List<NotesAssociationsRecord> getBySrcId(int srcId);

    List<NotesAssociationsRecord> getByDstId(int dstId);

    void save(NotesAssociationsRecord record);

    void delete(int id);

    void delete(int srcId, int dstId);
}
