package org.example.tonpad.core.repository;

import org.example.tonpad.core.models.NotesDataRecord;

import java.util.List;
import java.util.Optional;

public interface NotesDataRepository {

    List<NotesDataRecord> getAll();

    Optional<NotesDataRecord> getById(int id);

    void save(NotesDataRecord noteData);

    void delete(int id);
}
