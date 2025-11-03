package org.example.tonpad.core.repository;

import org.example.tonpad.core.models.NotesAssociationsRecord;

import java.util.List;
import java.util.Optional;

public interface NotesAssociationsRepository {

    List<NotesAssociationsRecord> getAll();

    Optional<NotesAssociationsRecord> getById(int id);

    List<NotesAssociationsRecord> getBySrcId(int srcId);

    List<NotesAssociationsRecord> getByDstId(int dstId);

    void save(NotesAssociationsRecord record);

    void delete(int id);

    void delete(int srcId, int dstId);
}
