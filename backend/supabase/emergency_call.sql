-- Run this once in the Supabase SQL editor for the existing WatchSafety DB.
-- The repository's full schema.sql already includes the same definitions.

begin;

alter table public.guardian_profiles
  add column if not exists emergency_phone_number text;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'guardian_profiles_emergency_phone_number_check'
      and conrelid = 'public.guardian_profiles'::regclass
  ) then
    alter table public.guardian_profiles
      add constraint guardian_profiles_emergency_phone_number_check
      check (
        emergency_phone_number is null
        or emergency_phone_number ~ '^\+?[0-9]{8,15}$'
      );
  end if;
end;
$$;

create or replace function public.get_watch_emergency_contact()
returns table (
  is_configured boolean,
  contact_name text,
  phone_number text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_watch_auth_id uuid := auth.uid();
begin
  if v_watch_auth_id is null then
    raise exception 'Authentication required';
  end if;

  return query
  select
    gp.emergency_phone_number is not null,
    gp.display_name,
    gp.emergency_phone_number
  from public.devices d
  join public.guardian_profiles gp
    on gp.guardian_id = d.guardian_id
  where d.watch_auth_id = v_watch_auth_id
  order by d.updated_at desc
  limit 1;

  if not found then
    return query
    select false, null::text, null::text;
  end if;
end;
$$;

revoke all on function public.get_watch_emergency_contact()
from public, anon;
grant execute on function public.get_watch_emergency_contact()
to authenticated;

commit;
