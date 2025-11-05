create table if not exists templates (
    id integer primary key autoincrement,
    name text not null
);

create table if not exists template_fields (
    id integer primary key autoincrement,
    template_id int not null,
    field text not null,

    CONSTRAINT template_fields_template_id_field_unique unique (template_id, field),
    foreign key(template_id) references templates(id)
);

create table if not exists notes (
    id integer primary key autoincrement,
    path text not null
);

create table if not exists notes_associations (
    id integer primary key autoincrement,
    src_id int not null,
    dst_id int not null,

    CONSTRAINT notes_associations_src_id_dst_id_unique unique (src_id, dst_id),
    foreign key(src_id) references notes(id),
    foreign key(dst_id) references notes(id)
);

create table if not exists notes_to_templates (
    id integer primary key autoincrement,
    note_id int not null,
    template_id int not null,

    CONSTRAINT notes_to_templates_note_id_template_id_unique unique (note_id, template_id),
    foreign key(note_id) references notes(id),
    foreign key(template_id) references templates(id)
);