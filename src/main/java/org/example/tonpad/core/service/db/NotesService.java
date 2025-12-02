package org.example.tonpad.core.service.db;

import org.example.tonpad.core.models.NoteRecord;

import java.util.List;

public interface NotesService {

    List<NoteRecord> getAll();

    NoteRecord getById(int id);

    NoteRecord getByPath(String path);

    void save(NoteRecord note);

    void delete(int id);

    void delete(String path);
}
