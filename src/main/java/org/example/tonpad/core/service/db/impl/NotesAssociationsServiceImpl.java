package org.example.tonpad.core.service.db.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.exceptions.ObjectNotFoundException;
import org.example.tonpad.core.models.NotesAssociationsRecord;
import org.example.tonpad.core.repository.NotesAssociationsRepository;
import org.example.tonpad.core.service.db.NotesAssociationsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotesAssociationsServiceImpl implements NotesAssociationsService {

    private final NotesAssociationsRepository repository;

    @Override
    public List<NotesAssociationsRecord> getAll() {
        return repository.getAll();
    }

    @Override
    public NotesAssociationsRecord getById(int id) {
        return repository.getById(id).orElseThrow(() -> new ObjectNotFoundException("Object not found"));
    }

    @Override
    public List<NotesAssociationsRecord> getBySrcId(int srcId) {
        return repository.getBySrcId(srcId);
    }

    @Override
    public List<NotesAssociationsRecord> getByDstId(int dstId) {
        return repository.getByDstId(dstId);
    }

    @Override
    @Transactional
    public void save(NotesAssociationsRecord record) {
        repository.save(record);
    }

    @Override
    @Transactional
    public void delete(int id) {
        repository.delete(id);
    }

    @Override
    @Transactional
    public void delete(int srcId, int dstId) {
        repository.delete(srcId, dstId);
    }
}
