alter table api_keys add column if not exists key_prefix varchar(32);
alter table api_keys add column if not exists expires_at timestamp;
alter table api_keys add column if not exists revoked_at timestamp;
alter table api_keys add column if not exists last_used_at timestamp;
alter table api_keys add column if not exists created_by varchar(160);

update api_keys set key_prefix = 'legacy' where key_prefix is null;
alter table api_keys alter column key_prefix set not null;

alter table tenant_tool_configs add column if not exists secret_ref varchar(500);
update tenant_tool_configs set secret_ref = signing_secret where secret_ref is null and signing_secret like 'env://%';
update tenant_tool_configs set active = false where secret_ref is null;
update tenant_tool_configs set signing_secret = '' where signing_secret is not null;

create table if not exists tenant_user_auth_configs (
    id uuid primary key,
    tenant_id uuid not null,
    issuer varchar(300) not null,
    audience varchar(160) not null,
    jwks_uri varchar(2000) not null,
    allowed_algorithm varchar(20) not null,
    max_token_lifetime_seconds integer not null,
    jwks_cache_ttl_seconds integer not null,
    active boolean not null,
    created_at timestamp not null,
    constraint fk_tenant_user_auth_configs_tenant foreign key (tenant_id) references tenants (id)
);

create index if not exists idx_api_keys_prefix on api_keys (key_prefix);
create index if not exists idx_tenant_user_auth_configs_tenant on tenant_user_auth_configs (tenant_id);
