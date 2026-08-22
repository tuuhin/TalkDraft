create type public.model_status as enum (
    'ACTIVE',
    'DEPRECATED',
    'DISABLED'
);

create type public.model_source as enum (
    'HUGGING_FACE'
);

create type public.model_format as enum (
    'GGML'
);

-- metadata model
create table public.model_catalog_metadata (
    id uuid primary key default gen_random_uuid(),

    model_family text not null,
    variant text not null,
    version text not null,

    display_name text not null,

    status public.model_status not null default 'ACTIVE',

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint model_catalog_metadata_identity_unique
        unique (model_family, variant, version)
);

-- artifact model
create table public.model_artifact (
    id uuid primary key default gen_random_uuid(),

    catalog_id uuid not null
        references public.model_catalog_metadata(id)
        on delete cascade,

    source public.model_source not null,

    repository text not null,
    revision text not null,
    artifact_path text not null,

    format public.model_format not null,

    languages text[] not null default '{}',

    size_bytes bigint not null,
    sha256 text not null,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint model_artifact_size_check
        check (size_bytes > 0),

    constraint model_artifact_sha256_check
        check (sha256 ~ '^[a-fA-F0-9]{64}$'),

    constraint model_artifact_unique
        unique (catalog_id, repository, revision, artifact_path)
);

-- Row Level Security
alter table public.model_catalog_metadata enable row level security;
alter table public.model_artifact enable row level security;

-- Client read access
grant select on public.model_catalog_metadata to anon;
grant select on public.model_catalog_metadata to authenticated;
grant select on public.model_artifact to anon;
grant select on public.model_artifact to authenticated;

create policy "Public can read model catalog metadata"
on public.model_catalog_metadata
for select
to anon, authenticated
using (true);

create policy "Public can read model artifacts"
on public.model_artifact
for select
to anon, authenticated
using (true);
