-- Ejecutar en Supabase → SQL Editor (después de pawpals_esquema_completo.sql).
-- Crea la fila mínima en public.usuarios para auth.uid(), sin depender solo del INSERT vía RLS.
-- La app llama: postgrest.rpc("pawpals_asegurar_mi_usuario", { p_correo, p_nombre_visible }).

create or replace function public.pawpals_asegurar_mi_usuario(
  p_correo text default '',
  p_nombre_visible text default 'Usuario'
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id text;
  v_correo text;
  v_nombre text;
  v_ts bigint;
begin
  v_id := (select auth.uid())::text;
  if v_id is null or v_id = '' then
    raise exception 'not_authenticated';
  end if;

  v_correo := coalesce(p_correo, '');
  v_nombre := coalesce(nullif(trim(p_nombre_visible), ''), '');
  if v_nombre = '' then
    v_nombre := 'Usuario';
  end if;
  v_ts := (floor(extract(epoch from clock_timestamp()) * 1000))::bigint;

  insert into public.usuarios (
    id,
    correo,
    nombre_visible,
    zona,
    sobre_mi,
    rol,
    bloqueado,
    creado_en
  )
  values (
    v_id,
    v_correo,
    v_nombre,
    '',
    '',
    'usuario',
    false,
    v_ts
  )
  on conflict (id) do nothing;
end;
$$;

revoke all on function public.pawpals_asegurar_mi_usuario(text, text) from public;
grant execute on function public.pawpals_asegurar_mi_usuario(text, text) to authenticated;

comment on function public.pawpals_asegurar_mi_usuario(text, text) is
  'Crea la fila del usuario actual en public.usuarios si no existe (SECURITY DEFINER).';
