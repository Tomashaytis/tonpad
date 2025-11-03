package org.example.tonpad.core.repository;

import org.example.tonpad.core.models.NotesToTemplatesRecord;

import java.util.List;
import java.util.Optional;

public interface NotesToTemplatesRepository {

    List<NotesToTemplatesRecord> getAll();

    Optional<NotesToTemplatesRecord> getById(int id);

    List<NotesToTemplatesRecord> getByNoteId(int noteId);

    List<NotesToTemplatesRecord> getByTemplateId(int templateId);

    void save(NotesToTemplatesRecord record);

    void delete(int id);
}
