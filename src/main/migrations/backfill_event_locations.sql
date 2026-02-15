-- Create legacy location row if it doesn't exist
insert into locations (id, name, description, lat, lon)
values (
    '69490405-e609-40f2-8d2c-92c5c9334ffa',
    'Legacy Location',
    'Legacy placeholder for existing events.',
    0.0,
    0.0
)
on conflict (id) do nothing;

-- Backfill all events to use the legacy location id
update events
set location_id = '69490405-e609-40f2-8d2c-92c5c9334ffa'
where location_id is null;

-- Drop old location column
alter table events
    drop column if exists location;
