-- Enable UUID generation (Supabase usually has this on)
create extension if not exists "pgcrypto";

-- Locations
create table if not exists locations (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    description text not null,
    lat double precision not null,
    lon double precision not null,
    created_at timestamptz not null default now()
);

-- Events now reference locations
alter table events
    add column if not exists location_id uuid references locations(id);
