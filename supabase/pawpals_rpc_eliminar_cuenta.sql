-- se ejecuta despues del esquema principal
-- borra la cuenta del usuario y sus datos relacionados

create or replace function public.eliminar_cuenta_auth()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_uid text := (select auth.uid())::text;
begin
  if v_uid is null or v_uid = '' then
    raise exception 'not authenticated';
  end if;

  delete from public.conversaciones
  where v_uid = any (participantes);

  delete from public.coincidencias
  where v_uid = any (participantes);

  delete from public.deslizamientos
  where uid_origen = v_uid or uid_destino = v_uid;

  delete from public.solicitudes_amistad
  where uid_origen = v_uid or uid_destino = v_uid;

  delete from public.amigos
  where usuario_id = v_uid or amigo_id = v_uid;

  delete from public.reportes
  where uid_reportante = v_uid;

  delete from public.tickets_soporte as t
  where t.uid = v_uid;

  delete from public.usuarios
  where id = v_uid;

  delete from auth.users
  where id = auth.uid();
end;
$$;

revoke all on function public.eliminar_cuenta_auth() from public;
grant execute on function public.eliminar_cuenta_auth() to authenticated;

comment on function public.eliminar_cuenta_auth() is
  'borra los datos del usuario y su cuenta de auth';
