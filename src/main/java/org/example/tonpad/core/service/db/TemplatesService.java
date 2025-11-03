package org.example.tonpad.core.service.db;

import org.example.tonpad.core.models.TemplateRecord;

import java.util.List;

public interface TemplatesService {

    List<TemplateRecord> getAll();

    TemplateRecord getById(int id);

    TemplateRecord getByName(String name);

    void save(TemplateRecord template);

    void delete(int id);
}
