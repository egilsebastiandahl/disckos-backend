-- Enable UUID generation (Supabase usually has this on)
create extension if not exists "pgcrypto";

-- Enums
do $$ begin
create type scoring_format as enum ('stroke', 'best_shot', 'worst_shot', 'alternate', 'match_play');
exception when duplicate_object then null;
end $$;

do $$ begin
create type event_type as enum ('team', 'individual');
exception when duplicate_object then null;
end $$;

-- Rounds
create table if not exists rounds (
                                      id uuid primary key default gen_random_uuid(),
    event_id uuid not null references events(id) on delete cascade,
    event_type event_type not null,
    scoring_format scoring_format not null,
    created_at timestamptz not null default now()
    );

-- Holes
create table if not exists holes (
                                     id uuid primary key default gen_random_uuid(),
    round_id uuid not null references rounds(id) on delete cascade,
    hole_number int not null,
    par int not null,
    scoring_format_override scoring_format null,
    created_at timestamptz not null default now(),
    unique (round_id, hole_number)
    );

-- Teams (minimal; tie teams to events)
create table if not exists teams (
                                     id uuid primary key default gen_random_uuid(),
    event_id uuid not null references events(id) on delete cascade,
    name text not null,
    created_at timestamptz not null default now()
    );

-- Individual player scores (for individual rounds)
create table if not exists player_scores (
                                             id uuid primary key default gen_random_uuid(),
    hole_id uuid not null references holes(id) on delete cascade,
    player_id uuid not null references players(id) on delete cascade,
    throws int not null,
    created_at timestamptz not null default now(),
    unique (hole_id, player_id)
    );

-- Team scores (for team rounds)
create table if not exists team_scores (
                                           id uuid primary key default gen_random_uuid(),
    hole_id uuid not null references holes(id) on delete cascade,
    team_id uuid not null references teams(id) on delete cascade,
    team_throws int not null,
    created_at timestamptz not null default now(),
    unique (hole_id, team_id)
    );

-- Member scores within a team score
create table if not exists team_member_scores (
                                                  id uuid primary key default gen_random_uuid(),
    team_score_id uuid not null references team_scores(id) on delete cascade,
    player_id uuid not null references players(id) on delete cascade,
    throws int not null,
    is_counted boolean not null default false,
    created_at timestamptz not null default now(),
    unique (team_score_id, player_id)
    );

-- Helpful indexes
create index if not exists idx_holes_round_id on holes(round_id);
create index if not exists idx_player_scores_hole_id on player_scores(hole_id);
create index if not exists idx_team_scores_hole_id on team_scores(hole_id);
create index if not exists idx_team_member_scores_team_score_id on team_member_scores(team_score_id);
