package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.TemplateFieldRecord;
import org.example.tonpad.core.repository.TemplateFieldsRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TemplateFieldsRepositoryImpl implements TemplateFieldsRepository {

    private final DSLContext ctx;

    @Override
    public List<TemplateFieldRecord> getAll() {
        return ctx
                .select()
                .from(TemplateFieldRecord.TEMPLATE_FIELDS)
                .fetchInto(TemplateFieldRecord.class);
    }

    @Override
    public Optional<TemplateFieldRecord> getById(int id) {
        return ctx
                .select()
                .from(TemplateFieldRecord.TEMPLATE_FIELDS)
                .where(TemplateFieldRecord.ID.eq(id))
                .fetchOptionalInto(TemplateFieldRecord.class);
    }

    @Override
    public void save(TemplateFieldRecord field) {
        if (field.getId() == null) {
            Integer id = ctx.insertInto(TemplateFieldRecord.TEMPLATE_FIELDS)
                    .set(TemplateFieldRecord.ID, field.getId())
                    .set(TemplateFieldRecord.FIELD, field.getField())
                    .returningResult(TemplateFieldRecord.ID)
                    .fetchOneInto(Integer.class);
            field.setId(id);
        } else {
            ctx.update(TemplateFieldRecord.TEMPLATE_FIELDS)
                    .set(TemplateFieldRecord.FIELD, field.getField())
                    .where(TemplateFieldRecord.ID.eq(field.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        ctx.deleteFrom(TemplateFieldRecord.TEMPLATE_FIELDS)
                .where(TemplateFieldRecord.ID.eq(id))
                .execute();
    }
}
