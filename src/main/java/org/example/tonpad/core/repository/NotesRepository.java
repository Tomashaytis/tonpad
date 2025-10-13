package org.example.tonpad.core.repository;

import org.example.tonpad.core.models.NotesRecord;
import org.jooq.Record;

import java.util.List;

public interface NotesRepository {

    List<NotesRecord> getAllNotes();
}
