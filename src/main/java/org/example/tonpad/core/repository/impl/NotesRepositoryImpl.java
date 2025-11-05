package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.NoteRecord;
import org.example.tonpad.core.repository.NotesRepository;
import org.example.tonpad.core.service.db.ConnectionProviderService;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotesRepositoryImpl implements NotesRepository {

    public static final Table<?> NOTES_TABLE = DSL.table("notes");

    public static final Field<Integer> ID_FIELD = DSL.field("id", Integer.class);
    public static final Field<String> NAME_FIELD = DSL.field("name", String.class);

    private final ConnectionProviderService connectionProviderService;

    @Override
    public List<NoteRecord> getAll() {
        DSLContext ctx = connectionProviderService.getDSLContext();

        return ctx.select()
                .from(NOTES_TABLE)
                .fetchInto(NoteRecord.class);
    }

    @Override
    public Optional<NoteRecord> getById(int id) {
        DSLContext ctx = connectionProviderService.getDSLContext();

        return ctx.select()
                .from(NOTES_TABLE)
                .where(ID_FIELD.eq(id))
                .fetchOptionalInto(NoteRecord.class);
    }

    @Override
    public void save(NoteRecord note) {
        DSLContext ctx = connectionProviderService.getDSLContext();

        if (note.getId() == null) {
            Integer id = ctx.insertInto(NOTES_TABLE)
                    .set(NAME_FIELD, note.getName())
                    .returningResult(ID_FIELD)
                    .fetchOneInto(Integer.class);
            note.setId(id);
        } else {
            ctx.update(NOTES_TABLE)
                    .set(NAME_FIELD, note.getName())
                    .where(ID_FIELD.eq(note.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        DSLContext ctx = connectionProviderService.getDSLContext();

        ctx.deleteFrom(NOTES_TABLE)
                .where(ID_FIELD.eq(id))
                .execute();
    }
}
