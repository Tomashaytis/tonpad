create table if not exists notes (
    id integer primary key autoincrement,
    name text not null,
    path text not null unique
);