package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.NotesToTemplatesRecord;
import org.example.tonpad.core.repository.NotesToTemplatesRepository;
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
public class NotesToTemplatesRepositoryImpl implements NotesToTemplatesRepository {

    public static final Table<?> NOTES_TO_TEMPLATES_TABLE = DSL.table("notes_to_templates");

    public static final Field<Integer> ID_FIELD = DSL.field("id", Integer.class);
    public static final Field<Integer> NOTE_ID_FIELD = DSL.field("note_id", Integer.class);
    public static final Field<Integer> TEMPLATE_ID_FIELD = DSL.field("template_id", Integer.class);

    private final ConnectionProviderService connectionProviderService;

    private final VaultPath vaultPath;

    @Override
    public List<NotesToTemplatesRecord> getAll() {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        return ctx.select()
                .from(NOTES_TO_TEMPLATES_TABLE)
                .fetchInto(NotesToTemplatesRecord.class);
    }

    @Override
    public Optional<NotesToTemplatesRecord> getById(int id) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        return ctx.select()
                .from(NOTES_TO_TEMPLATES_TABLE)
                .where(ID_FIELD.eq(id))
                .fetchOptionalInto(NotesToTemplatesRecord.class);
    }

    @Override
    public List<NotesToTemplatesRecord> getByNoteId(int noteId) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        return ctx.select()
                .from(NOTES_TO_TEMPLATES_TABLE)
                .where(NOTE_ID_FIELD.eq(noteId))
                .fetchInto(NotesToTemplatesRecord.class);
    }

    @Override
    public List<NotesToTemplatesRecord> getByTemplateId(int templateId) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        return ctx.select()
                .from(NOTES_TO_TEMPLATES_TABLE)
                .where(TEMPLATE_ID_FIELD.eq(templateId))
                .fetchInto(NotesToTemplatesRecord.class);
    }

    @Override
    public void save(NotesToTemplatesRecord record) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        if (record.getId() == null) {
            Integer id = ctx.insertInto(NOTES_TO_TEMPLATES_TABLE)
                    .set(NOTE_ID_FIELD, record.getNoteId())
                    .set(TEMPLATE_ID_FIELD, record.getTemplateId())
                    .returningResult(ID_FIELD)
                    .fetchOneInto(Integer.class);
            record.setId(id);
        } else {
            ctx.update(NOTES_TO_TEMPLATES_TABLE)
                    .set(NOTE_ID_FIELD, record.getNoteId())
                    .set(TEMPLATE_ID_FIELD, record.getTemplateId())
                    .where(ID_FIELD.eq(record.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        DSLContext ctx = connectionProviderService.getDSLContext(vaultPath.getVaultPath());

        ctx.delete(NOTES_TO_TEMPLATES_TABLE)
                .where(ID_FIELD.eq(id))
                .execute();
    }
}
