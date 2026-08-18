-- Watch Safety - Supabase schema
-- Apply to a new Supabase project with the SQL editor or Supabase CLI.

create table public.guardian_profiles (
  guardian_id uuid primary key references auth.users (id) on delete cascade,
  display_name text not null,
  relationship text not null default '보호자',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.wearers (
  id uuid primary key default gen_random_uuid(),
  guardian_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  phone_number text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, guardian_id)
);

create table public.guardian_pairings (
  id uuid primary key default gen_random_uuid(),
  guardian_id uuid not null references auth.users (id) on delete cascade,
  wearer_id uuid not null,
  relationship text not null default '딸',
  created_at timestamptz not null default now(),
  unique (guardian_id, wearer_id),
  foreign key (wearer_id, guardian_id)
    references public.wearers (id, guardian_id) on delete cascade
);

create table public.devices (
  id uuid primary key default gen_random_uuid(),
  guardian_id uuid not null references auth.users (id) on delete cascade,
  wearer_id uuid not null,
  device_name text not null,
  battery_percent integer not null default 100 check (battery_percent between 0 and 100),
  is_connected boolean not null default false,
  is_wearing boolean not null default false,
  last_connected_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, guardian_id),
  foreign key (wearer_id, guardian_id)
    references public.wearers (id, guardian_id) on delete cascade
);

create table public.locations (
  id bigint generated always as identity primary key,
  guardian_id uuid not null references auth.users (id) on delete cascade,
  wearer_id uuid not null,
  device_id uuid,
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  accuracy_meters double precision,
  address text not null,
  short_address text not null,
  safe_zone_name text,
  is_inside_safe_zone boolean not null default false,
  recorded_at timestamptz not null default now(),
  foreign key (wearer_id, guardian_id)
    references public.wearers (id, guardian_id) on delete cascade,
  foreign key (device_id, guardian_id)
    references public.devices (id, guardian_id) on delete set null (device_id)
);

create table public.safe_zones (
  id uuid primary key default gen_random_uuid(),
  guardian_id uuid not null references auth.users (id) on delete cascade,
  wearer_id uuid not null,
  name text not null,
  address text not null,
  center_latitude double precision not null check (center_latitude between -90 and 90),
  center_longitude double precision not null check (center_longitude between -180 and 180),
  radius_meters integer not null check (radius_meters between 100 and 1000),
  enabled boolean not null default true,
  kind text not null default 'OTHER'
    check (kind in ('HOME', 'CARE_CENTER', 'HOSPITAL', 'OTHER')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  foreign key (wearer_id, guardian_id)
    references public.wearers (id, guardian_id) on delete cascade
);

create table public.safety_events (
  id uuid primary key default gen_random_uuid(),
  guardian_id uuid not null references auth.users (id) on delete cascade,
  wearer_id uuid not null,
  device_id uuid,
  type text not null check (type in (
    'SAFE_ZONE_EXITED',
    'SAFE_ZONE_ENTERED',
    'FALL_SUSPECTED',
    'FALL_CONFIRMED_SAFE',
    'SOS_MANUAL',
    'SOS_AUTOMATIC',
    'RETURN_HOME_REQUESTED',
    'NAVIGATION_STARTED',
    'ARRIVED_HOME',
    'BATTERY_LOW',
    'DEVICE_STATUS_UPDATED'
  )),
  title text not null,
  description text not null,
  latitude double precision,
  longitude double precision,
  address text,
  metadata jsonb not null default '{}'::jsonb,
  occurred_at timestamptz not null default now(),
  foreign key (wearer_id, guardian_id)
    references public.wearers (id, guardian_id) on delete cascade,
  foreign key (device_id, guardian_id)
    references public.devices (id, guardian_id) on delete set null (device_id)
);

create table public.return_home_requests (
  id uuid primary key default gen_random_uuid(),
  guardian_id uuid not null references auth.users (id) on delete cascade,
  wearer_id uuid not null,
  status text not null default 'REQUESTED'
    check (status in ('REQUESTED', 'ACCEPTED', 'NAVIGATING', 'ARRIVED', 'CANCELLED')),
  requested_at timestamptz not null default now(),
  responded_at timestamptz,
  foreign key (wearer_id, guardian_id)
    references public.wearers (id, guardian_id) on delete cascade
);

create table public.notification_settings (
  guardian_id uuid primary key references auth.users (id) on delete cascade,
  sos_alert boolean not null default true,
  safe_zone_exit_alert boolean not null default true,
  arrival_alert boolean not null default true,
  battery_low_alert boolean not null default false,
  updated_at timestamptz not null default now()
);

create index locations_guardian_wearer_recorded_idx
  on public.locations (guardian_id, wearer_id, recorded_at desc);
create index safe_zones_guardian_wearer_idx
  on public.safe_zones (guardian_id, wearer_id);
create index safety_events_guardian_wearer_occurred_idx
  on public.safety_events (guardian_id, wearer_id, occurred_at desc);
create index return_home_guardian_wearer_requested_idx
  on public.return_home_requests (guardian_id, wearer_id, requested_at desc);

-- Cover every foreign key in both column orders used by joins and deletes.
create index devices_guardian_id_idx on public.devices (guardian_id);
create index devices_wearer_guardian_idx on public.devices (wearer_id, guardian_id);
create index guardian_pairings_wearer_guardian_idx
  on public.guardian_pairings (wearer_id, guardian_id);
create index locations_device_guardian_idx on public.locations (device_id, guardian_id);
create index locations_wearer_guardian_idx on public.locations (wearer_id, guardian_id);
create index return_home_requests_wearer_guardian_idx
  on public.return_home_requests (wearer_id, guardian_id);
create index safe_zones_wearer_guardian_idx on public.safe_zones (wearer_id, guardian_id);
create index safety_events_device_guardian_idx
  on public.safety_events (device_id, guardian_id);
create index safety_events_wearer_guardian_idx
  on public.safety_events (wearer_id, guardian_id);
create index wearers_guardian_id_idx on public.wearers (guardian_id);

alter table public.guardian_profiles enable row level security;
alter table public.wearers enable row level security;
alter table public.guardian_pairings enable row level security;
alter table public.devices enable row level security;
alter table public.locations enable row level security;
alter table public.safe_zones enable row level security;
alter table public.safety_events enable row level security;
alter table public.return_home_requests enable row level security;
alter table public.notification_settings enable row level security;

-- Every authenticated guardian can access only rows carrying their own auth.uid().
create policy guardian_profiles_select_own on public.guardian_profiles
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy guardian_profiles_insert_own on public.guardian_profiles
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy guardian_profiles_update_own on public.guardian_profiles
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);

create policy wearers_select_own on public.wearers
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy wearers_insert_own on public.wearers
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy wearers_update_own on public.wearers
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);
create policy wearers_delete_own on public.wearers
  for delete to authenticated using ((select auth.uid()) = guardian_id);

create policy guardian_pairings_select_own on public.guardian_pairings
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy guardian_pairings_insert_own on public.guardian_pairings
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy guardian_pairings_update_own on public.guardian_pairings
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);
create policy guardian_pairings_delete_own on public.guardian_pairings
  for delete to authenticated using ((select auth.uid()) = guardian_id);

create policy devices_select_own on public.devices
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy devices_insert_own on public.devices
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy devices_update_own on public.devices
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);
create policy devices_delete_own on public.devices
  for delete to authenticated using ((select auth.uid()) = guardian_id);

create policy locations_select_own on public.locations
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy locations_insert_own on public.locations
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy locations_update_own on public.locations
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);
create policy locations_delete_own on public.locations
  for delete to authenticated using ((select auth.uid()) = guardian_id);

create policy safe_zones_select_own on public.safe_zones
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy safe_zones_insert_own on public.safe_zones
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy safe_zones_update_own on public.safe_zones
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);
create policy safe_zones_delete_own on public.safe_zones
  for delete to authenticated using ((select auth.uid()) = guardian_id);

create policy safety_events_select_own on public.safety_events
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy safety_events_insert_own on public.safety_events
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy safety_events_update_own on public.safety_events
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);
create policy safety_events_delete_own on public.safety_events
  for delete to authenticated using ((select auth.uid()) = guardian_id);

create policy return_home_requests_select_own on public.return_home_requests
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy return_home_requests_insert_own on public.return_home_requests
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy return_home_requests_update_own on public.return_home_requests
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);
create policy return_home_requests_delete_own on public.return_home_requests
  for delete to authenticated using ((select auth.uid()) = guardian_id);

create policy notification_settings_select_own on public.notification_settings
  for select to authenticated using ((select auth.uid()) = guardian_id);
create policy notification_settings_insert_own on public.notification_settings
  for insert to authenticated with check ((select auth.uid()) = guardian_id);
create policy notification_settings_update_own on public.notification_settings
  for update to authenticated
  using ((select auth.uid()) = guardian_id)
  with check ((select auth.uid()) = guardian_id);

revoke all on table
  public.guardian_profiles,
  public.wearers,
  public.guardian_pairings,
  public.devices,
  public.locations,
  public.safe_zones,
  public.safety_events,
  public.return_home_requests,
  public.notification_settings
from anon;

grant select, insert, update, delete on table
  public.guardian_profiles,
  public.wearers,
  public.guardian_pairings,
  public.devices,
  public.locations,
  public.safe_zones,
  public.safety_events,
  public.return_home_requests,
  public.notification_settings
to authenticated;

grant usage, select on sequence public.locations_id_seq to authenticated;

create or replace function public.bootstrap_guardian_demo()
returns uuid
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
declare
  v_guardian_id uuid := auth.uid();
  v_wearer_id uuid;
  v_device_id uuid;
begin
  if v_guardian_id is null then
    raise exception 'Authentication is required';
  end if;

  insert into public.guardian_profiles (guardian_id, display_name, relationship)
  values (v_guardian_id, '보호자', '딸')
  on conflict (guardian_id) do nothing;

  select id into v_wearer_id
  from public.wearers
  where guardian_id = v_guardian_id
  order by created_at
  limit 1;

  if v_wearer_id is not null then
    return v_wearer_id;
  end if;

  insert into public.wearers (guardian_id, name, phone_number)
  values (v_guardian_id, '김순자', '01012345678')
  returning id into v_wearer_id;

  insert into public.guardian_pairings (guardian_id, wearer_id, relationship)
  values (v_guardian_id, v_wearer_id, '딸');

  insert into public.devices (
    guardian_id, wearer_id, device_name, battery_percent,
    is_connected, is_wearing, last_connected_at
  ) values (
    v_guardian_id, v_wearer_id, 'Galaxy Watch7', 78,
    true, true, now()
  ) returning id into v_device_id;

  insert into public.locations (
    guardian_id, wearer_id, device_id, latitude, longitude, accuracy_meters,
    address, short_address, safe_zone_name, is_inside_safe_zone, recorded_at
  ) values (
    v_guardian_id, v_wearer_id, v_device_id, 37.5665, 126.9780, 15.0,
    '서울시 행복구 행복동 123-4 인근', '행복동 자택 근처', '집', true,
    now() - interval '2 minutes'
  );

  insert into public.safe_zones (
    guardian_id, wearer_id, name, address, center_latitude,
    center_longitude, radius_meters, enabled, kind
  ) values
    (v_guardian_id, v_wearer_id, '집', '서울시 행복구 행복동 123-4', 37.5665, 126.9780, 500, true, 'HOME'),
    (v_guardian_id, v_wearer_id, '행복 복지관', '행복구 복지로 22', 37.5684, 126.9810, 300, true, 'CARE_CENTER'),
    (v_guardian_id, v_wearer_id, '행복 병원', '행복구 건강길 8', 37.5638, 126.9750, 300, false, 'HOSPITAL');

  insert into public.safety_events (
    guardian_id, wearer_id, device_id, type, title, description,
    latitude, longitude, address, occurred_at
  ) values
    (v_guardian_id, v_wearer_id, v_device_id, 'ARRIVED_HOME', '집 도착', '집 안전구역에 진입했어요', 37.5665, 126.9780, '행복동 자택', now() - interval '30 minutes'),
    (v_guardian_id, v_wearer_id, v_device_id, 'RETURN_HOME_REQUESTED', '귀가 안내 시작', '보호자 요청으로 안내 시작', 37.5680, 126.9800, '행복동 인근', now() - interval '50 minutes'),
    (v_guardian_id, v_wearer_id, v_device_id, 'SAFE_ZONE_EXITED', '안전구역 이탈', '집 구역에서 120m 벗어남', 37.5690, 126.9820, '행복동 인근', now() - interval '62 minutes'),
    (v_guardian_id, v_wearer_id, v_device_id, 'FALL_CONFIRMED_SAFE', '낙상 의심 → 정상 확인', '사용자가 괜찮음을 선택', 37.5670, 126.9790, '행복동 인근', now() - interval '4 hours'),
    (v_guardian_id, v_wearer_id, v_device_id, 'SOS_MANUAL', '수동 SOS', '사용자가 SOS 버튼을 눌렀어요', 37.5650, 126.9760, '행복구 소망공원', now() - interval '1 day'),
    (v_guardian_id, v_wearer_id, v_device_id, 'BATTERY_LOW', '배터리 부족 20%', '충전 안내가 전송되었어요', null, null, null, now() - interval '1 day 4 hours');

  insert into public.notification_settings (
    guardian_id, sos_alert, safe_zone_exit_alert, arrival_alert, battery_low_alert
  ) values (v_guardian_id, true, true, true, false);

  return v_wearer_id;
end;
$$;

revoke execute on function public.bootstrap_guardian_demo() from public, anon;
grant execute on function public.bootstrap_guardian_demo() to authenticated;

alter publication supabase_realtime add table
  public.devices,
  public.locations,
  public.safe_zones,
  public.safety_events,
  public.return_home_requests,
  public.notification_settings;
