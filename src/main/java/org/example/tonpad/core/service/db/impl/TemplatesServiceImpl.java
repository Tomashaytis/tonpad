package org.example.tonpad.core.service.db.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.exceptions.ObjectNotFoundException;
import org.example.tonpad.core.models.TemplateFieldRecord;
import org.example.tonpad.core.models.TemplateRecord;
import org.example.tonpad.core.repository.TemplateFieldsRepository;
import org.example.tonpad.core.repository.TemplatesRepository;
import org.example.tonpad.core.service.db.TemplatesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplatesServiceImpl implements TemplatesService {

    private final TemplatesRepository templatesRepository;

    private final TemplateFieldsRepository fieldsRepository;

    @Override
    public List<TemplateRecord> getAll() {
        List<TemplateRecord> templates = templatesRepository.getAll();
        for (TemplateRecord record : templates) {
            record.setFields(fieldsRepository.getByTemplateId(record.getId()));
        }
        return templates;
    }

    @Override
    public TemplateRecord getById(int id) {
        TemplateRecord template = templatesRepository.getById(id).orElseThrow(() -> new ObjectNotFoundException("Произошла ошибка"));
        template.setFields(fieldsRepository.getByTemplateId(id));
        return template;
    }

    @Override
    public TemplateRecord getByName(String name) {
        TemplateRecord template = templatesRepository.getByName(name).orElseThrow(() -> new ObjectNotFoundException("Произошла ошибка"));
        template.setFields(fieldsRepository.getByTemplateId(template.getId()));
        return template;
    }

    @Override
    @Transactional
    public void save(TemplateRecord template) {
        templatesRepository.save(template);
        fieldsRepository.deleteByTemplateId(template.getId());
        for (String field : template.getFields()) {
            fieldsRepository.save(new TemplateFieldRecord(field));
        }
    }

    @Override
    @Transactional
    public void delete(int id) {
        templatesRepository.delete(id);
        fieldsRepository.deleteByTemplateId(id);
    }
}
