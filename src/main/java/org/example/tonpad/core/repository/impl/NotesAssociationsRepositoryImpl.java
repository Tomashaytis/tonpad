package org.example.tonpad.core.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.models.NotesAssociationsRecord;
import org.example.tonpad.core.repository.NotesAssociationsRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotesAssociationsRepositoryImpl implements NotesAssociationsRepository {

    public static final Table<?> ASSOCIATIONS_TABLE = DSL.table("notes_associations");

    public static final Field<Integer> ID_FIELD = DSL.field("id", Integer.class);
    public static final Field<Integer> SRC_ID_FIELD = DSL.field("src_id", Integer.class);
    public static final Field<Integer> DST_ID_FIELD = DSL.field("dst_id", Integer.class);

    private final DSLContext ctx;

    @Override
    public List<NotesAssociationsRecord> getAll() {
        return ctx.select()
                .from(ASSOCIATIONS_TABLE)
                .fetchInto(NotesAssociationsRecord.class);
    }

    @Override
    public Optional<NotesAssociationsRecord> getById(int id) {
        return ctx.select()
                .from(ASSOCIATIONS_TABLE)
                .where(ID_FIELD.eq(id))
                .fetchOptionalInto(NotesAssociationsRecord.class);
    }

    @Override
    public List<NotesAssociationsRecord> getBySrcId(int srcId) {
        return ctx.select()
                .from(ASSOCIATIONS_TABLE)
                .where(SRC_ID_FIELD.eq(srcId))
                .fetchInto(NotesAssociationsRecord.class);
    }

    @Override
    public List<NotesAssociationsRecord> getByDstId(int dstId) {
        return ctx.select()
                .from(ASSOCIATIONS_TABLE)
                .where(DST_ID_FIELD.eq(dstId))
                .fetchInto(NotesAssociationsRecord.class);
    }

    @Override
    public void save(NotesAssociationsRecord record) {
        if (record.getId() == null) {
            Integer id = ctx.insertInto(ASSOCIATIONS_TABLE)
                    .set(SRC_ID_FIELD, record.getSrcId())
                    .set(DST_ID_FIELD, record.getDstId())
                    .returningResult(ID_FIELD)
                    .fetchOneInto(Integer.class);
            record.setId(id);
        } else {
            ctx.update(ASSOCIATIONS_TABLE)
                    .set(SRC_ID_FIELD, record.getSrcId())
                    .set(DST_ID_FIELD, record.getDstId())
                    .where(ID_FIELD.eq(record.getId()))
                    .execute();
        }
    }

    @Override
    public void delete(int id) {
        ctx.delete(ASSOCIATIONS_TABLE)
                .where(ID_FIELD.eq(id))
                .execute();
    }

    @Override
    public void delete(int srcId, int dstId) {
        ctx.delete(ASSOCIATIONS_TABLE)
                .where(SRC_ID_FIELD.eq(srcId), DST_ID_FIELD.eq(dstId))
                .execute();
    }
}
