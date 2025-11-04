package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.TemplateRecord;
import org.example.tonpad.core.repository.TemplatesRepository;
import org.example.tonpad.core.service.db.ConnectionProviderService;
import org.example.tonpad.ui.extentions.VaultPath;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TemplatesRepositoryImpl implements TemplatesRepository {

    public static final Table<?> TEMPLATES_TABLE = DSL.table("templates");

    public static final Field<Integer> ID_FIELD = DSL.field("id", Integer.class);
    public static final Field<String> NAME_FIELD = DSL.field("name", String.class);

    private final ConnectionProviderService connectionProviderService;

    private final VaultPath vaultPath;

    @Override
    public List<TemplateRecord> getAll() {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        return ctx.select()
                .from(TEMPLATES_TABLE)
                .fetchInto(TemplateRecord.class);
    }

    @Override
    public Optional<TemplateRecord> getById(int id) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        return ctx.select()
                .from(TEMPLATES_TABLE)
                .where(ID_FIELD.eq(id))
                .fetchOptionalInto(TemplateRecord.class);
    }

    @Override
    public Optional<TemplateRecord> getByName(String name) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        return ctx.select()
                .from(TEMPLATES_TABLE)
                .where(NAME_FIELD.eq(name))
                .fetchOptionalInto(TemplateRecord.class);
    }

    @Override
    public void save(TemplateRecord template) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        if (template.getId() == null) {
            Integer id = ctx.insertInto(TEMPLATES_TABLE)
                    .set(NAME_FIELD, template.getName())
                    .returningResult(ID_FIELD)
                    .fetchOneInto(Integer.class);
            template.setId(id);
        } else {
            ctx.update(TEMPLATES_TABLE)
                    .set(NAME_FIELD, template.getName())
                    .where(ID_FIELD.eq(template.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        ctx.deleteFrom(TEMPLATES_TABLE)
                .where(ID_FIELD.eq(id))
                .execute();
    }
}
