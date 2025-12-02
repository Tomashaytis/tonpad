package org.example.tonpad.core.service.db.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.exceptions.ObjectNotFoundException;
import org.example.tonpad.core.models.NoteRecord;
import org.example.tonpad.core.repository.NotesRepository;
import org.example.tonpad.core.service.db.NotesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotesServiceImpl implements NotesService {

    private final NotesRepository repository;

    @Override
    public List<NoteRecord> getAll() {
        return repository.getAll();
    }

    @Override
    public NoteRecord getById(int id) {
        return repository.getById(id).orElseThrow(() -> new ObjectNotFoundException("Object not found"));
    }

    @Override
    public NoteRecord getByPath(String path) {
        return repository.getByPath(path).orElseThrow(() -> new ObjectNotFoundException("Произошла ошибка"));
    }

    @Override
    @Transactional
    public void save(NoteRecord note) {
        repository.save(note);
    }

    @Override
    @Transactional
    public void delete(int id) {
        repository.delete(id);
    }

    @Override
    @Transactional
    public void delete(String path) {
        repository.delete(path);
    }
}
