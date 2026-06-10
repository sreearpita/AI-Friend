create table if not exists tenants (
    id uuid primary key,
    slug varchar(80) not null unique,
    display_name varchar(160) not null,
    active boolean not null,
    created_at timestamp not null
);

create table if not exists api_keys (
    id uuid primary key,
    tenant_id uuid not null,
    key_hash varchar(128) not null unique,
    label varchar(120) not null,
    active boolean not null,
    created_at timestamp not null,
    constraint fk_api_keys_tenant foreign key (tenant_id) references tenants (id)
);

create table if not exists chat_sessions (
    id uuid primary key,
    tenant_id uuid not null,
    external_user_id varchar(128) not null,
    derived_preference_summary varchar(2000),
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_chat_sessions_tenant foreign key (tenant_id) references tenants (id)
);

create table if not exists chat_messages (
    id uuid primary key,
    tenant_id uuid not null,
    session_id uuid not null,
    role varchar(20) not null,
    content text not null,
    safety_status varchar(40) not null,
    created_at timestamp not null,
    constraint fk_chat_messages_tenant foreign key (tenant_id) references tenants (id),
    constraint fk_chat_messages_session foreign key (session_id) references chat_sessions (id)
);

create table if not exists audit_events (
    id uuid primary key,
    tenant_id uuid not null,
    external_user_id varchar(128),
    session_id uuid,
    event_type varchar(120) not null,
    metadata_json text not null,
    created_at timestamp not null
);

create table if not exists tenant_tool_configs (
    id uuid primary key,
    tenant_id uuid not null,
    name varchar(80) not null,
    callback_url varchar(500) not null,
    signing_secret varchar(500) not null,
    signing_key_id varchar(80) not null,
    active boolean not null,
    created_at timestamp not null,
    constraint fk_tenant_tool_configs_tenant foreign key (tenant_id) references tenants (id),
    constraint uq_tenant_tool_configs_tenant_name unique (tenant_id, name)
);

create table if not exists tenant_tool_allowed_scopes (
    tool_config_id uuid not null,
    scope varchar(80) not null,
    constraint fk_tenant_tool_allowed_scopes_config foreign key (tool_config_id) references tenant_tool_configs (id)
);

create index if not exists idx_api_keys_tenant_id on api_keys (tenant_id);
create index if not exists idx_chat_sessions_tenant_user on chat_sessions (tenant_id, external_user_id);
create index if not exists idx_chat_messages_session_created on chat_messages (session_id, created_at);
create index if not exists idx_audit_events_tenant_created on audit_events (tenant_id, created_at);
create index if not exists idx_tenant_tool_configs_tenant on tenant_tool_configs (tenant_id);
