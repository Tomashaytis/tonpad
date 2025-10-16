package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.TemplateRecord;
import org.example.tonpad.core.repository.TemplatesRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TemplatesRepositoryImpl implements TemplatesRepository {

    private final DSLContext ctx;

    @Override
    public List<TemplateRecord> getAll() {
        return ctx
                .select()
                .from(TemplateRecord.TEMPLATES)
                .fetchInto(TemplateRecord.class);
    }

    @Override
    public Optional<TemplateRecord> getById(int id) {
        return ctx
                .select()
                .from(TemplateRecord.TEMPLATES)
                .where(TemplateRecord.ID.eq(id))
                .fetchOptionalInto(TemplateRecord.class);
    }

    @Override
    public void save(TemplateRecord template) {
        if (template.getId() == null) {
            Integer id = ctx.insertInto(TemplateRecord.TEMPLATES)
                    .set(TemplateRecord.ID, template.getId())
                    .set(TemplateRecord.NAME, template.getName())
                    .returningResult(TemplateRecord.ID)
                    .fetchOneInto(Integer.class);
            template.setId(id);
        } else {
            ctx.update(TemplateRecord.TEMPLATES)
                    .set(TemplateRecord.NAME, template.getName())
                    .where(TemplateRecord.ID.eq(template.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        ctx.deleteFrom(TemplateRecord.TEMPLATES)
                .where(TemplateRecord.ID.eq(id))
                .execute();
    }
}
