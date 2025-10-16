package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.NotesDataRecord;
import org.example.tonpad.core.repository.NotesDataRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotesDataRepositoryImpl implements NotesDataRepository {

    private final DSLContext ctx;

    @Override
    public List<NotesDataRecord> getAll() {
        return ctx
                .select()
                .from(NotesDataRecord.NOTES_DATA)
                .fetchInto(NotesDataRecord.class);
    }

    @Override
    public Optional<NotesDataRecord> getById(int id) {
        return ctx
                .select()
                .from(NotesDataRecord.NOTES_DATA)
                .where(NotesDataRecord.ID.eq(id))
                .fetchOptionalInto(NotesDataRecord.class);
    }

    @Override
    public void save(NotesDataRecord noteData) {
        if (noteData.getId() == null) {
            Integer id = ctx.insertInto(NotesDataRecord.NOTES_DATA)
                    .set(NotesDataRecord.ID, noteData.getId())
                    .set(NotesDataRecord.NOTE_PATH, noteData.getNotePath())
                    .set(NotesDataRecord.TEMPLATE_ID, noteData.getTemplateId())
                    .set(NotesDataRecord.FIELD, noteData.getField())
                    .set(NotesDataRecord.VALUE, noteData.getValue())
                    .returningResult(NotesDataRecord.ID)
                    .fetchOneInto(Integer.class);
            noteData.setId(id);
        } else {
            ctx.update(NotesDataRecord.NOTES_DATA)
                    .set(NotesDataRecord.NOTE_PATH, noteData.getNotePath())
                    .set(NotesDataRecord.TEMPLATE_ID, noteData.getTemplateId())
                    .set(NotesDataRecord.FIELD, noteData.getField())
                    .set(NotesDataRecord.VALUE, noteData.getValue())
                    .where(NotesDataRecord.ID.eq(noteData.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        ctx.deleteFrom(NotesDataRecord.NOTES_DATA)
                .where(NotesDataRecord.ID.eq(id))
                .execute();
    }
}
