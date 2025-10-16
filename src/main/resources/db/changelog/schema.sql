create table if not exists templates (
    id integer primary key autoincrement,
    name text not null
);

create table if not exists template_fields (
    id integer primary key autoincrement,
    field text not null
);

create table if not exists notes_data (
    id integer primary key autoincrement,
    note_path text not null,
    template_id integer not null,
    field text not null,
    value text
);