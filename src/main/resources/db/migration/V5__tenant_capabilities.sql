alter table tenant_tool_configs add column if not exists contract_version varchar(160);
update tenant_tool_configs set contract_version = case
    when name = 'cycle-summary' then 'flowelle.cycle-summary.v1'
    when name = 'user-preferences' then 'flowelle.user-preferences.v1'
    else 'generic.host-tool.v1'
end where contract_version is null;
alter table tenant_tool_configs alter column contract_version set not null;

create table if not exists tenant_capabilities (
    id uuid primary key,
    tenant_id uuid not null,
    capability_key varchar(100) not null,
    display_name varchar(160) not null,
    description varchar(500) not null,
    priority integer not null,
    active boolean not null,
    created_at timestamp not null,
    constraint fk_tenant_capabilities_tenant foreign key (tenant_id) references tenants(id),
    constraint uq_tenant_capabilities_tenant_key unique (tenant_id, capability_key)
);
create table if not exists tenant_capability_triggers (capability_id uuid not null, trigger_phrase varchar(120) not null, constraint fk_capability_triggers foreign key (capability_id) references tenant_capabilities(id));
create table if not exists tenant_capability_scopes (capability_id uuid not null, scope varchar(80) not null, constraint fk_capability_scopes foreign key (capability_id) references tenant_capabilities(id));
create table if not exists tenant_capability_tools (capability_id uuid not null, tool_name varchar(80) not null, tool_order integer not null, constraint fk_capability_tools foreign key (capability_id) references tenant_capabilities(id));
create table if not exists tenant_capability_topics (capability_id uuid not null, topic varchar(100) not null, constraint fk_capability_topics foreign key (capability_id) references tenant_capabilities(id));
create index if not exists idx_tenant_capabilities_tenant on tenant_capabilities(tenant_id, active, priority);
