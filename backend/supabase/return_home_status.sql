-- 귀가 안내 도중 뒤로가기로 취소할 수 있도록 허용한다.
-- 호출 워치와 요청의 보호자/착용자 관계 검증은 유지한다.
create or replace function public.update_watch_return_home_status(
    p_request_id uuid,
    p_target_status text
)
returns void
language plpgsql
security definer
set search_path to ''
as $function$
declare
    v_updated_count integer;
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    if p_target_status not in ('ACCEPTED', 'CANCELLED', 'NAVIGATING', 'COMPLETED') then
        raise exception 'Invalid target status: %', p_target_status;
    end if;

    update public.return_home_requests r
    set status = p_target_status,
        responded_at = case
            when p_target_status in ('ACCEPTED', 'CANCELLED')
                then coalesce(r.responded_at, now())
            else r.responded_at
        end
    where r.id = p_request_id
      and exists (
          select 1 from public.devices d
          where d.watch_auth_id = auth.uid()
            and d.guardian_id = r.guardian_id
            and d.wearer_id = r.wearer_id
      )
      and (
          (r.status = 'REQUESTED' and p_target_status in ('ACCEPTED', 'CANCELLED'))
          or (r.status = 'ACCEPTED' and p_target_status in ('NAVIGATING', 'CANCELLED'))
          or (r.status = 'NAVIGATING' and p_target_status in ('COMPLETED', 'CANCELLED'))
      );

    get diagnostics v_updated_count = row_count;

    if v_updated_count = 0 then
        -- 응답 유실 후 취소 재시도나 도착 완료와의 경합은 성공으로 처리한다.
        -- 이미 완료된 요청을 CANCELLED로 덮어쓰지는 않는다.
        if p_target_status = 'CANCELLED' and exists (
            select 1 from public.return_home_requests r
            join public.devices d
              on d.guardian_id = r.guardian_id and d.wearer_id = r.wearer_id
            where r.id = p_request_id
              and d.watch_auth_id = auth.uid()
              and r.status in ('CANCELLED', 'COMPLETED')
        ) then
            return;
        end if;
        raise exception 'Return home status update failed. request_id=%, target=%',
            p_request_id, p_target_status;
    end if;
end;
$function$;

