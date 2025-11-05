package org.example.tonpad.core.service.db;

import org.example.tonpad.core.models.NotesToTemplatesRecord;

import java.util.List;

public interface NotesToTemplatesService {

    List<NotesToTemplatesRecord> getAll();

    NotesToTemplatesRecord getById(int id);

    List<NotesToTemplatesRecord> getByNoteId(int noteId);

    List<NotesToTemplatesRecord> getByTemplateId(int templateId);

    void save(NotesToTemplatesRecord record);

    void delete(int id);
}
