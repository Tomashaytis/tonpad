package org.example.tonpad.core.repository;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.NotesRecord;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Класс-пример создания запросов <br>
 * <br>
 * Для построения запроса используется класс DSLContext <br>
 * Для select опционально указываются поля, определенные через DSL.field <br>
 * Для from указывается таблица через DSL.table <br>
 * См. прмиеры в классе NotesRecord <br>
 * <br>
 * Для возвращения результата есть 2 варианта: заранее написанная модель, как NotesRecord,
 * либо можно использовать класс org.jooq.Record и его наследники, но есть нюанс.
 * Если надо вернуть List своих моделей, то надо в конце запроса использовать fetchInto(ModelName.class),
 * а если надо вернуть List из org.jooq.Record, то нужно использовать .fetchStream().toList()
 */
@Repository
@RequiredArgsConstructor
public class NotesRepositoryImpl implements NotesRepository {

    private final DSLContext ctx;

    public List<NotesRecord> getAllNotes() {
        return ctx
                .select()
                .from(NotesRecord.NOTES)
                .fetchInto(NotesRecord.class);
    }

    public void insertNote(NotesRecord note) {
        ctx.insertInto(NotesRecord.NOTES)
                .set(NotesRecord.ID, note.getId())
                .set(NotesRecord.NAME, note.getName())
                .set(NotesRecord.PATH, note.getPath())
                .execute();
    }

    public void updateNote(NotesRecord note) {
        ctx.update(NotesRecord.NOTES)
                .set(NotesRecord.ID, note.getId())
                .set(NotesRecord.NAME, note.getName())
                .set(NotesRecord.PATH, note.getPath())
                .where(NotesRecord.ID.eq(note.getId()))
                .execute();
    }

    public void deleteNote(int id) {
        ctx.deleteFrom(NotesRecord.NOTES)
                .where(NotesRecord.ID.eq(id))
                .execute();
    }
}
