-- Ejecutar en Supabase → SQL (tras [pawpals_esquema_completo.sql]).
-- Permite a la app llamar a postgrest.rpc("eliminar_cuenta_auth") tras reautenticar:
-- borra filas públicas ligadas al uid y el registro en auth.users.

create or replace function public.eliminar_cuenta_auth()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  uid text := (select auth.uid())::text;
begin
  if uid is null or uid = '' then
    raise exception 'not authenticated';
  end if;

  -- Conversaciones del usuario (mensajes en cascada)
  delete from public.conversaciones
  where uid = any(participantes);

  delete from public.coincidencias
  where uid = any(participantes);

  delete from public.deslizamientos
  where uid_origen = uid or uid_destino = uid;

  delete from public.solicitudes_amistad
  where uid_origen = uid or uid_destino = uid;

  delete from public.reportes
  where uid_reportante = uid;

  delete from public.tickets_soporte
  where uid = uid;

  -- amigos y perros se limpian por on delete cascade al borrar usuarios
  delete from public.usuarios
  where id = uid;

  delete from auth.users
  where id = auth.uid();
end;
$$;

revoke all on function public.eliminar_cuenta_auth() from public;
grant execute on function public.eliminar_cuenta_auth() to authenticated;

comment on function public.eliminar_cuenta_auth() is
  'Auto-borrado de cuenta: datos públicos + auth.users (SECURITY DEFINER).';
