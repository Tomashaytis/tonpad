package org.example.tonpad.core.service.db.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.exceptions.ObjectNotFoundException;
import org.example.tonpad.core.models.NotesToTemplatesRecord;
import org.example.tonpad.core.repository.NotesToTemplatesRepository;
import org.example.tonpad.core.service.db.NotesToTemplatesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotesToTemplatesServiceImpl implements NotesToTemplatesService {

    private final NotesToTemplatesRepository repository;

    @Override
    public List<NotesToTemplatesRecord> getAll() {
        return repository.getAll();
    }

    @Override
    public NotesToTemplatesRecord getById(int id) {
        return repository.getById(id).orElseThrow(() -> new ObjectNotFoundException("Object not found"));
    }

    @Override
    public List<NotesToTemplatesRecord> getByNoteId(int noteId) {
        return repository.getByNoteId(noteId);
    }

    @Override
    public List<NotesToTemplatesRecord> getByTemplateId(int templateId) {
        return repository.getByTemplateId(templateId);
    }

    @Override
    @Transactional
    public void save(NotesToTemplatesRecord record) {
        repository.save(record);
    }

    @Override
    @Transactional
    public void delete(int id) {
        repository.delete(id);
    }
}
