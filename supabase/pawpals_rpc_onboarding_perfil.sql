-- se ejecuta despues del esquema y del script de asegurar usuario
-- guarda los datos del onboarding sin pelearse con las reglas de seguridad

-- actualiza los datos de la persona
create or replace function public.pawpals_actualizar_mi_perfil(
  p_nombre_visible text,
  p_zona text,
  p_sobre_mi text,
  p_url_foto text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid text := (select auth.uid())::text;
  v_n int;
begin
  if v_uid is null or v_uid = '' then
    raise exception 'not_authenticated';
  end if;

  update public.usuarios u set
    nombre_visible = p_nombre_visible,
    zona = p_zona,
    sobre_mi = p_sobre_mi,
    url_foto = coalesce(p_url_foto, u.url_foto)
  where u.id = v_uid;

  get diagnostics v_n = row_count;
  if v_n = 0 then
    raise exception 'user_row_missing';
  end if;
end;
$$;

revoke all on function public.pawpals_actualizar_mi_perfil(text, text, text, text) from public;
grant execute on function public.pawpals_actualizar_mi_perfil(text, text, text, text) to authenticated;

comment on function public.pawpals_actualizar_mi_perfil(text, text, text, text) is
  'actualiza nombre, zona, descripcion y foto del usuario';

-- crea o actualiza el perro del usuario
create or replace function public.pawpals_guardar_mi_perro(
  p_nombre text,
  p_raza text,
  p_edad_anios int,
  p_biografia text default '',
  p_url_foto text default null,
  p_energia text default 'moderado',
  p_sociabilidad text default 'muy_sociable'
)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid text := (select auth.uid())::text;
  v_id text;
  v_ts bigint;
begin
  if v_uid is null or v_uid = '' then
    raise exception 'not_authenticated';
  end if;

  v_ts := (floor(extract(epoch from clock_timestamp()) * 1000))::bigint;

  select p.id into v_id
  from public.perros p
  where p.uid_dueno = v_uid
  limit 1;

  if v_id is null then
    v_id := gen_random_uuid()::text;
    insert into public.perros (
      id,
      uid_dueno,
      nombre,
      raza,
      edad_anios,
      biografia,
      url_foto,
      energia,
      sociabilidad,
      actualizado_en
    )
    values (
      v_id,
      v_uid,
      coalesce(p_nombre, ''),
      coalesce(p_raza, ''),
      greatest(coalesce(p_edad_anios, 1), 1),
      coalesce(p_biografia, ''),
      p_url_foto,
      coalesce(nullif(trim(p_energia), ''), 'moderado'),
      coalesce(nullif(trim(p_sociabilidad), ''), 'muy_sociable'),
      v_ts
    );
  else
    update public.perros r set
      nombre = coalesce(p_nombre, ''),
      raza = coalesce(p_raza, ''),
      edad_anios = greatest(coalesce(p_edad_anios, 1), 1),
      biografia = coalesce(p_biografia, ''),
      url_foto = coalesce(p_url_foto, r.url_foto),
      energia = coalesce(nullif(trim(p_energia), ''), r.energia),
      sociabilidad = coalesce(nullif(trim(p_sociabilidad), ''), r.sociabilidad),
      actualizado_en = v_ts
    where r.id = v_id;
  end if;

  return v_id;
end;
$$;

revoke all on function public.pawpals_guardar_mi_perro(text, text, int, text, text, text, text) from public;
grant execute on function public.pawpals_guardar_mi_perro(text, text, int, text, text, text, text) to authenticated;

comment on function public.pawpals_guardar_mi_perro(text, text, int, text, text, text, text) is
  'crea o actualiza el perro del usuario y devuelve su id';
