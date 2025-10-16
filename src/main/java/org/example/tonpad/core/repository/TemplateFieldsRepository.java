package org.example.tonpad.core.repository;

import org.example.tonpad.core.models.TemplateFieldRecord;

import java.util.List;
import java.util.Optional;

public interface TemplateFieldsRepository {

    List<TemplateFieldRecord> getAll();

    Optional<TemplateFieldRecord> getById(int id);

    void save(TemplateFieldRecord field);

    void delete(int id);
}
