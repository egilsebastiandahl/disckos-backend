-- Team Templates: reusable team definitions independent of events
create table if not exists team_templates (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Team Template Members: which players belong to a template
create table if not exists team_template_members (
    team_template_id uuid not null references team_templates(id) on delete cascade,
    player_id uuid not null references players(id) on delete cascade,
    added_at timestamptz not null default now(),
    primary key (team_template_id, player_id)
);

-- Link existing event-scoped teams to a template (optional)
alter table teams
    add column if not exists team_template_id uuid references team_templates(id) on delete set null;

-- Indexes
create index if not exists idx_team_template_members_template on team_template_members(team_template_id);
create index if not exists idx_team_template_members_player on team_template_members(player_id);
create index if not exists idx_teams_template_id on teams(team_template_id);
