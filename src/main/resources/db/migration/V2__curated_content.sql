create table if not exists content_sources (
    id uuid primary key,
    title varchar(240) not null,
    publisher varchar(160) not null,
    url varchar(500) not null,
    locale varchar(20) not null,
    review_status varchar(40) not null,
    active boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists content_chunks (
    id uuid primary key,
    source_id uuid not null,
    topic varchar(120) not null,
    chunk_text text not null,
    keywords varchar(1000) not null,
    review_status varchar(40) not null,
    active boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_content_chunks_source foreign key (source_id) references content_sources (id)
);

create index if not exists idx_content_sources_status_locale on content_sources (review_status, active, locale);
create index if not exists idx_content_chunks_status_topic on content_chunks (review_status, active, topic);
create index if not exists idx_content_chunks_source on content_chunks (source_id);
