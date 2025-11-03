package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.TemplateFieldRecord;
import org.example.tonpad.core.repository.TemplateFieldsRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TemplateFieldsRepositoryImpl implements TemplateFieldsRepository {



    public static final Table<?> TEMPLATE_FIELDS_TABLE = DSL.table("template_fields");

    public static final Field<Integer> ID_FIELD = DSL.field("id", Integer.class);
    public static final Field<Integer> TEMPLATE_ID_FIELD = DSL.field("template_id", Integer.class);
    public static final Field<String> FIELD_FIELD = DSL.field("field", String.class);

    private final DSLContext ctx;

    @Override
    public List<TemplateFieldRecord> getAll() {
        return ctx
                .select()
                .from(TEMPLATE_FIELDS_TABLE)
                .fetchInto(TemplateFieldRecord.class);
    }

    @Override
    public Optional<TemplateFieldRecord> getById(int id) {
        return ctx
                .select()
                .from(TEMPLATE_FIELDS_TABLE)
                .where(ID_FIELD.eq(id))
                .fetchOptionalInto(TemplateFieldRecord.class);
    }

    public List<String> getByTemplateId(int templateId) {
        return ctx.select(FIELD_FIELD)
                .from(TEMPLATE_FIELDS_TABLE)
                .where(TEMPLATE_ID_FIELD.eq(templateId))
                .fetchInto(String.class);
    }

    @Override
    public void save(TemplateFieldRecord field) {
        if (field.getId() == null) {
            Integer id = ctx.insertInto(TEMPLATE_FIELDS_TABLE)
                    .set(FIELD_FIELD, field.getField())
                    .returningResult(ID_FIELD)
                    .fetchOneInto(Integer.class);
            field.setId(id);
        } else {
            ctx.update(TEMPLATE_FIELDS_TABLE)
                    .set(FIELD_FIELD, field.getField())
                    .where(ID_FIELD.eq(field.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        ctx.deleteFrom(TEMPLATE_FIELDS_TABLE)
                .where(ID_FIELD.eq(id))
                .execute();
    }

    public void deleteByTemplateId(int templateId) {
        ctx.deleteFrom(TEMPLATE_FIELDS_TABLE)
                .where(TEMPLATE_ID_FIELD.eq(templateId))
                .execute();
    }
}
