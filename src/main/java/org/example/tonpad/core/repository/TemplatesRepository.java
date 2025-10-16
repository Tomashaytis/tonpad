package org.example.tonpad.core.repository;

import org.example.tonpad.core.models.TemplateRecord;

import java.util.List;
import java.util.Optional;

public interface TemplatesRepository {

    List<TemplateRecord> getAll();

    Optional<TemplateRecord> getById(int id);

    void save(TemplateRecord template);

    void delete(int id);
}
