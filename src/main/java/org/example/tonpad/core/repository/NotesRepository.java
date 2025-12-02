package org.example.tonpad.core.repository;

import org.example.tonpad.core.models.NoteRecord;

import java.util.List;
import java.util.Optional;

public interface NotesRepository {

    List<NoteRecord> getAll();

    Optional<NoteRecord> getById(int id);

    Optional<NoteRecord> getByPath(String path);

    void save(NoteRecord note);

    void delete(int id);

    void delete(String path);
}
