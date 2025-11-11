-- SQLite Schema Validation Script
-- Проверяет соответствие базы данных заданной схеме
-- Возвращает строки с ошибками, если есть несоответствия

-- Проверка существования таблицы templates
SELECT 'Table "templates" does not exist' AS error
    WHERE NOT EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='templates')

UNION ALL

-- Проверка структуры таблицы templates
SELECT 'Table "templates" has incorrect structure' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='templates')
  AND (
    (SELECT COUNT(*) FROM pragma_table_info('templates')
     WHERE (name='id' AND type='INTEGER' AND pk=1)
        OR (name='name' AND type='TEXT' AND "notnull"=1)) != 2
    OR (SELECT COUNT(*) FROM pragma_table_info('templates')) != 2
  )

UNION ALL

-- Проверка существования таблицы template_fields
SELECT 'Table "template_fields" does not exist' AS error
    WHERE NOT EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='template_fields')

UNION ALL

-- Проверка структуры таблицы template_fields
SELECT 'Table "template_fields" has incorrect structure' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='template_fields')
  AND (
    (SELECT COUNT(*) FROM pragma_table_info('template_fields')
     WHERE (name='id' AND type='INTEGER' AND pk=1)
        OR (name='template_id' AND type='INT' AND "notnull"=1)
        OR (name='field' AND type='TEXT' AND "notnull"=1)) != 3
    OR (SELECT COUNT(*) FROM pragma_table_info('template_fields')) != 3
  )

UNION ALL

-- Проверка UNIQUE (template_id, field) в template_fields
SELECT 'Missing UNIQUE constraint on (template_id, field) in template_fields' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='template_fields')
  AND NOT EXISTS (
    SELECT 1 FROM sqlite_schema
    WHERE name = 'template_fields'
      AND sql LIKE '%CONSTRAINT template_fields_template_id_field_unique UNIQUE (template_id, field)%'
  )

UNION ALL

-- Проверка внешнего ключа template_fields -> templates
SELECT 'Missing foreign key: template_fields.template_id -> templates.id' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='template_fields')
  AND NOT EXISTS (
    SELECT 1 FROM pragma_foreign_key_list('template_fields')
    WHERE "table"='templates' AND "from"='template_id'
  )

UNION ALL

-- Проверка существования таблицы notes
SELECT 'Table "notes" does not exist' AS error
    WHERE NOT EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes')

UNION ALL

-- Проверка структуры таблицы notes
SELECT 'Table "notes" has incorrect structure' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes')
  AND (
    (SELECT COUNT(*) FROM pragma_table_info('notes')
     WHERE (name='id' AND type='INTEGER' AND pk=1)
        OR (name='path' AND type='TEXT' AND "notnull"=1)) != 2
    OR (SELECT COUNT(*) FROM pragma_table_info('notes')) != 2
  )

UNION ALL

-- Проверка существования таблицы notes_associations
SELECT 'Table "notes_associations" does not exist' AS error
    WHERE NOT EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_associations')

UNION ALL

-- Проверка структуры таблицы notes_associations
SELECT 'Table "notes_associations" has incorrect structure' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_associations')
  AND (
    (SELECT COUNT(*) FROM pragma_table_info('notes_associations')
     WHERE (name='id' AND type='INTEGER' AND pk=1)
        OR (name='src_id' AND type='INT' AND "notnull"=1)
        OR (name='dst_id' AND type='INT' AND "notnull"=1)) != 3
    OR (SELECT COUNT(*) FROM pragma_table_info('notes_associations')) != 3
  )

UNION ALL

-- Проверка UNIQUE (src_id, dst_id) в notes_associations
SELECT 'Missing UNIQUE constraint on (src_id, dst_id) in notes_associations' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_associations')
  AND NOT EXISTS (
    SELECT 1 FROM sqlite_schema
    WHERE name = 'notes_associations'
      AND sql LIKE '%CONSTRAINT notes_associations_src_id_dst_id_unique UNIQUE (src_id, dst_id)%'
  )

UNION ALL

-- Проверка внешних ключей notes_associations -> notes
SELECT 'Missing foreign keys: notes_associations -> notes' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_associations')
  AND (SELECT COUNT(*) FROM pragma_foreign_key_list('notes_associations')
       WHERE "table"='notes' AND "from" IN ('src_id', 'dst_id')) != 2

UNION ALL

-- Проверка существования таблицы notes_to_templates
SELECT 'Table "notes_to_templates" does not exist' AS error
    WHERE NOT EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_to_templates')

UNION ALL

-- Проверка структуры таблицы notes_to_templates
SELECT 'Table "notes_to_templates" has incorrect structure' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_to_templates')
  AND (
    (SELECT COUNT(*) FROM pragma_table_info('notes_to_templates')
     WHERE (name='id' AND type='INTEGER' AND pk=1)
        OR (name='note_id' AND type='INT' AND "notnull"=1)
        OR (name='template_id' AND type='INT' AND "notnull"=1)) != 3
    OR (SELECT COUNT(*) FROM pragma_table_info('notes_to_templates')) != 3
  )

UNION ALL

-- Проверка UNIQUE (note_id, template_id) в notes_to_templates
SELECT 'Missing UNIQUE constraint on (note_id, template_id) in notes_to_templates' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_to_templates')
  AND NOT EXISTS (
    SELECT 1 FROM sqlite_schema
    WHERE name = 'notes_to_templates'
      AND sql LIKE '%CONSTRAINT notes_to_templates_note_id_template_id_unique UNIQUE (note_id, template_id)%'
  )

UNION ALL

-- Проверка внешнего ключа notes_to_templates.note_id -> notes.id
SELECT 'Missing foreign key: notes_to_templates.note_id -> notes.id' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_to_templates')
  AND NOT EXISTS (
    SELECT 1 FROM pragma_foreign_key_list('notes_to_templates')
    WHERE "table"='notes' AND "from"='note_id'
  )

UNION ALL

-- Проверка внешнего ключа notes_to_templates.template_id -> templates.id
SELECT 'Missing foreign key: notes_to_templates.template_id -> templates.id' AS error
    WHERE EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='notes_to_templates')
  AND NOT EXISTS (
    SELECT 1 FROM pragma_foreign_key_list('notes_to_templates')
    WHERE "table"='templates' AND "from"='template_id'
  );