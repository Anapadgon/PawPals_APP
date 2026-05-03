-- =============================================================================
-- PawPals — Esquema Postgres + RLS + Storage para Supabase
-- =============================================================================
-- Ejecuta TODO este archivo en: Supabase Dashboard → SQL Editor → New query.
--
-- No incluyas aquí URL ni claves API (anon/service_role). Configúralas solo en
-- la app (BuildConfig / local.properties / secrets de CI).
--
-- Convenciones:
--   - Columnas en snake_case; mapeo desde la app Kotlin/Firestore indicado en
--     comentarios (-- app: nombreCampo).
--   - IDs de usuario como TEXT para igualar auth.uid()::text y UIDs demo.
-- =============================================================================

-- Opcional: pgcrypto ya suele estar en Supabase para gen_random_uuid().
create extension if not exists "pgcrypto";

-- Nota: las funciones que consultan tablas van DESPUÉS de crear las tablas
-- (PostgreSQL valida que existan al crear la función).

-- -----------------------------------------------------------------------------
-- Tablas
-- -----------------------------------------------------------------------------

create table if not exists public.usuarios (
  id text primary key, -- auth.uid()::text o IDs demo
  correo text not null default '',
  nombre_visible text not null default '', -- app: nombreVisible
  zona text not null default '',
  sobre_mi text not null default '', -- app: sobreMi
  rol text not null default 'usuario', -- 'usuario' | 'administrador' | 'administradoristrador'
  bloqueado boolean not null default false,
  latitud double precision,
  longitud double precision,
  ubicacion_actualizada_en bigint, -- app: ubicacionActualizadaEn (epoch ms)
  token_fcm text, -- app: tokenFcm
  url_foto text, -- app: urlFoto
  numero_amigos integer not null default 0, -- app: numeroAmigos
  numero_paseos integer not null default 0, -- app: numeroPaseos
  numero_coincidencias integer not null default 0, -- app: numeroCoincidencias
  paseando boolean not null default false,
  creado_en bigint, -- app: creadoEn (epoch ms)
  es_demo boolean not null default false -- app: demo
);

create index if not exists idx_usuarios_es_demo on public.usuarios (es_demo) where es_demo = true;

create table if not exists public.perros (
  id text primary key,
  uid_dueno text not null references public.usuarios (id) on delete cascade, -- app: uidDueno
  nombre text not null default '',
  raza text not null default '',
  edad_anios integer not null default 1, -- app: edadAnios
  biografia text not null default '',
  url_foto text, -- app: urlFoto
  energia text not null default 'moderado', -- app: energia (enum en minúsculas)
  sociabilidad text not null default 'muy_sociable', -- app: sociabilidad
  actualizado_en bigint, -- app: actualizadoEn
  es_demo boolean not null default false -- app: demo
);

create index if not exists idx_perros_uid_dueno on public.perros (uid_dueno);

create table if not exists public.coincidencias (
  id text primary key,
  usuario_a text not null, -- app: usuarioA (orden menor)
  usuario_b text not null, -- app: usuarioB (orden mayor)
  usuario_menor text not null, -- app: usuarioMenor
  usuario_mayor text not null, -- app: usuarioMayor
  participantes text[] not null, -- exactamente dos ids
  iniciador text not null, -- app: iniciador (uid que originó la coincidencia)
  estado text not null default 'pendiente', -- pendiente | aceptada | rechazada
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint -- app: creadoEn
);

create index if not exists idx_coincidencias_participantes on public.coincidencias using gin (participantes);
create index if not exists idx_coincidencias_menor_mayor on public.coincidencias (usuario_menor, usuario_mayor);

create table if not exists public.deslizamientos (
  id text primary key, -- app: "<uidOrigen>__<uidDestino>"
  uid_origen text not null,
  uid_destino text not null,
  accion text not null, -- me_gusta | super_me_gusta | descartar
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_desliz_origen on public.deslizamientos (uid_origen);

create table if not exists public.solicitudes_amistad (
  id text primary key, -- app: "<uidOrigen>__<uidDestino>"
  uid_origen text not null,
  uid_destino text not null,
  estado text not null default 'pendiente', -- pendiente | aceptada | rechazada
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_solicitudes_destino on public.solicitudes_amistad (uid_destino, estado);

-- Sustituye la subcolección usuarios/{uid}/amigos/{otroUid}
create table if not exists public.amigos (
  usuario_id text not null references public.usuarios (id) on delete cascade,
  amigo_id text not null references public.usuarios (id) on delete cascade,
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint, -- app: creadoEn
  primary key (usuario_id, amigo_id)
);

create index if not exists idx_amigos_amigo on public.amigos (amigo_id);

create table if not exists public.tickets_soporte (
  id uuid primary key default gen_random_uuid(),
  uid text not null, -- creador (debe coincidir con auth al insertar)
  correo text not null,
  mensaje text not null,
  estado text not null default 'open', -- open | …
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

-- Metadatos del hilo (mensajes en tabla aparte, igual que subcolección Firestore)
create table if not exists public.conversaciones (
  id text primary key, -- app: "<uidBajo>__<uidAlto>"
  participantes text[] not null,
  creado_en bigint,
  ultimo_mensaje text,
  ultimo_mensaje_en bigint, -- app: ultimoMensajeEn
  ultimo_remitente text -- app: ultimoRemitente
);

create table if not exists public.mensajes (
  id uuid primary key default gen_random_uuid(),
  conversacion_id text not null references public.conversaciones (id) on delete cascade,
  uid_remitente text not null, -- app: uidRemitente
  texto text not null default '',
  marca_temporal bigint not null default (floor(extract(epoch from now()) * 1000))::bigint -- app: marcaTemporal
);

create index if not exists idx_mensajes_conversacion_tiempo on public.mensajes (conversacion_id, marca_temporal);

create table if not exists public.reportes (
  id uuid primary key default gen_random_uuid(),
  tipo_objetivo text not null, -- usuario | mensaje | perro  (app: tipoObjetivo)
  id_objetivo text not null, -- app: idObjetivo
  uid_reportante text not null, -- app: uidReportante
  motivo text not null,
  estado text not null default 'abierto', -- abierto | revisado | accion_tomada
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_reportes_creado on public.reportes (creado_en desc);

-- -----------------------------------------------------------------------------
-- Funciones auxiliares (después de tablas; usadas en políticas RLS)
-- -----------------------------------------------------------------------------

create or replace function public.pawpals_usuario_autenticado()
returns boolean
language sql
stable
as $$
  select auth.uid() is not null;
$$;

create or replace function public.pawpals_mismo_usuario(uid text)
returns boolean
language sql
stable
as $$
  select auth.uid() is not null and (auth.uid())::text = uid;
$$;

-- Compatibilidad: la app escribe "administrador"; las reglas antiguas usaban typo.
create or replace function public.pawpals_es_administrador()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.usuarios u
    where u.id = (auth.uid())::text
      and u.rol in ('administrador', 'administradoristrador')
  );
$$;

-- Para políticas de mensajes: el usuario pertenece al hilo de conversación.
create or replace function public.pawpals_participa_conversacion(p_conversacion_id text)
returns boolean
language sql
stable
as $$
  select exists (
    select 1
    from public.conversaciones c
    where c.id = p_conversacion_id
      and (auth.uid())::text = any (c.participantes)
  );
$$;

comment on function public.pawpals_es_administrador() is 'True si usuarios.rol es administrador (incluye typo legado).';

-- -----------------------------------------------------------------------------
-- Row Level Security
-- -----------------------------------------------------------------------------

alter table public.usuarios enable row level security;
alter table public.perros enable row level security;
alter table public.coincidencias enable row level security;
alter table public.deslizamientos enable row level security;
alter table public.solicitudes_amistad enable row level security;
alter table public.amigos enable row level security;
alter table public.tickets_soporte enable row level security;
alter table public.conversaciones enable row level security;
alter table public.mensajes enable row level security;
alter table public.reportes enable row level security;

-- --- usuarios ---
drop policy if exists usuarios_select_autenticado on public.usuarios;
create policy usuarios_select_autenticado
  on public.usuarios for select
  to authenticated
  using (public.pawpals_usuario_autenticado());

drop policy if exists usuarios_insert_propio_o_admin on public.usuarios;
create policy usuarios_insert_propio_o_admin
  on public.usuarios for insert
  to authenticated
  with check (
    public.pawpals_mismo_usuario(id) or public.pawpals_es_administrador()
  );

drop policy if exists usuarios_update_propio_o_admin on public.usuarios;
create policy usuarios_update_propio_o_admin
  on public.usuarios for update
  to authenticated
  using (public.pawpals_mismo_usuario(id) or public.pawpals_es_administrador())
  with check (public.pawpals_mismo_usuario(id) or public.pawpals_es_administrador());

drop policy if exists usuarios_delete_propio_o_admin on public.usuarios;
create policy usuarios_delete_propio_o_admin
  on public.usuarios for delete
  to authenticated
  using (public.pawpals_mismo_usuario(id) or public.pawpals_es_administrador());

-- --- perros ---
drop policy if exists perros_select_autenticado on public.perros;
create policy perros_select_autenticado
  on public.perros for select
  to authenticated
  using (public.pawpals_usuario_autenticado());

drop policy if exists perros_insert_dueno_o_admin on public.perros;
create policy perros_insert_dueno_o_admin
  on public.perros for insert
  to authenticated
  with check (
    (auth.uid())::text = uid_dueno or public.pawpals_es_administrador()
  );

drop policy if exists perros_update_dueno_o_admin on public.perros;
create policy perros_update_dueno_o_admin
  on public.perros for update
  to authenticated
  using ((auth.uid())::text = uid_dueno or public.pawpals_es_administrador())
  with check ((auth.uid())::text = uid_dueno or public.pawpals_es_administrador());

drop policy if exists perros_delete_dueno_o_admin on public.perros;
create policy perros_delete_dueno_o_admin
  on public.perros for delete
  to authenticated
  using ((auth.uid())::text = uid_dueno or public.pawpals_es_administrador());

-- --- coincidencias ---
drop policy if exists coincidencias_select_autenticado on public.coincidencias;
create policy coincidencias_select_autenticado
  on public.coincidencias for select
  to authenticated
  using (public.pawpals_usuario_autenticado());

drop policy if exists coincidencias_insert_participante on public.coincidencias;
create policy coincidencias_insert_participante
  on public.coincidencias for insert
  to authenticated
  with check (
    (auth.uid())::text = any (participantes)
  );

drop policy if exists coincidencias_update_participante on public.coincidencias;
create policy coincidencias_update_participante
  on public.coincidencias for update
  to authenticated
  using ((auth.uid())::text = any (participantes))
  with check ((auth.uid())::text = any (participantes));

drop policy if exists coincidencias_delete_participante_o_admin on public.coincidencias;
create policy coincidencias_delete_participante_o_admin
  on public.coincidencias for delete
  to authenticated
  using (
    public.pawpals_es_administrador()
    or (auth.uid())::text = any (participantes)
  );

-- --- deslizamientos ---
drop policy if exists desliz_select_autenticado on public.deslizamientos;
create policy desliz_select_autenticado
  on public.deslizamientos for select
  to authenticated
  using (public.pawpals_usuario_autenticado());

drop policy if exists desliz_insert_update_origen on public.deslizamientos;
create policy desliz_insert_update_origen
  on public.deslizamientos for insert
  to authenticated
  with check ((auth.uid())::text = uid_origen);

drop policy if exists desliz_update_origen on public.deslizamientos;
create policy desliz_update_origen
  on public.deslizamientos for update
  to authenticated
  using ((auth.uid())::text = uid_origen)
  with check ((auth.uid())::text = uid_origen);

drop policy if exists desliz_delete_condiciones on public.deslizamientos;
create policy desliz_delete_condiciones
  on public.deslizamientos for delete
  to authenticated
  using (
    public.pawpals_es_administrador()
    or (auth.uid())::text = uid_origen
    or (auth.uid())::text = uid_destino
  );

-- --- solicitudes_amistad ---
drop policy if exists sol_select_autenticado on public.solicitudes_amistad;
create policy sol_select_autenticado
  on public.solicitudes_amistad for select
  to authenticated
  using (public.pawpals_usuario_autenticado());

drop policy if exists sol_insert_origen on public.solicitudes_amistad;
create policy sol_insert_origen
  on public.solicitudes_amistad for insert
  to authenticated
  with check ((auth.uid())::text = uid_origen);

drop policy if exists sol_update_partes on public.solicitudes_amistad;
create policy sol_update_partes
  on public.solicitudes_amistad for update
  to authenticated
  using (
    (auth.uid())::text = uid_origen
    or (auth.uid())::text = uid_destino
  )
  with check (
    (auth.uid())::text = uid_origen
    or (auth.uid())::text = uid_destino
  );

drop policy if exists sol_delete_admin on public.solicitudes_amistad;
create policy sol_delete_admin
  on public.solicitudes_amistad for delete
  to authenticated
  using (public.pawpals_es_administrador());

-- --- amigos ---
drop policy if exists amigos_select_autenticado on public.amigos;
create policy amigos_select_autenticado
  on public.amigos for select
  to authenticated
  using (public.pawpals_usuario_autenticado());

drop policy if exists amigos_insert_implicados on public.amigos;
create policy amigos_insert_implicados
  on public.amigos for insert
  to authenticated
  with check (
    (auth.uid())::text = usuario_id
    or (auth.uid())::text = amigo_id
  );

drop policy if exists amigos_delete_implicados on public.amigos;
create policy amigos_delete_implicados
  on public.amigos for delete
  to authenticated
  using (
    (auth.uid())::text = usuario_id
    or (auth.uid())::text = amigo_id
    or public.pawpals_es_administrador()
  );

drop policy if exists amigos_update_admin on public.amigos;
create policy amigos_update_admin
  on public.amigos for update
  to authenticated
  using (public.pawpals_es_administrador())
  with check (public.pawpals_es_administrador());

-- --- tickets_soporte ---
drop policy if exists tickets_select_admin on public.tickets_soporte;
create policy tickets_select_admin
  on public.tickets_soporte for select
  to authenticated
  using (public.pawpals_es_administrador());

drop policy if exists tickets_insert_propietario on public.tickets_soporte;
create policy tickets_insert_propietario
  on public.tickets_soporte for insert
  to authenticated
  with check ((auth.uid())::text = uid);

drop policy if exists tickets_update_delete_admin on public.tickets_soporte;
create policy tickets_update_delete_admin
  on public.tickets_soporte for update
  to authenticated
  using (public.pawpals_es_administrador())
  with check (public.pawpals_es_administrador());

drop policy if exists tickets_delete_admin on public.tickets_soporte;
create policy tickets_delete_admin
  on public.tickets_soporte for delete
  to authenticated
  using (public.pawpals_es_administrador());

-- --- conversaciones ---
drop policy if exists conv_select_autenticado on public.conversaciones;
create policy conv_select_autenticado
  on public.conversaciones for select
  to authenticated
  using (public.pawpals_usuario_autenticado());

drop policy if exists conv_insert_participante on public.conversaciones;
create policy conv_insert_participante
  on public.conversaciones for insert
  to authenticated
  with check ((auth.uid())::text = any (participantes));

drop policy if exists conv_update_participante on public.conversaciones;
create policy conv_update_participante
  on public.conversaciones for update
  to authenticated
  using ((auth.uid())::text = any (participantes))
  with check ((auth.uid())::text = any (participantes));

-- --- mensajes ---
drop policy if exists msg_select_participante on public.mensajes;
create policy msg_select_participante
  on public.mensajes for select
  to authenticated
  using (public.pawpals_participa_conversacion(conversacion_id));

drop policy if exists msg_insert_remitente on public.mensajes;
create policy msg_insert_remitente
  on public.mensajes for insert
  to authenticated
  with check (
    (auth.uid())::text = uid_remitente
    and public.pawpals_participa_conversacion(conversacion_id)
  );

drop policy if exists msg_update_delete_admin on public.mensajes;
create policy msg_update_delete_admin
  on public.mensajes for update
  to authenticated
  using (public.pawpals_es_administrador())
  with check (public.pawpals_es_administrador());

drop policy if exists msg_delete_admin on public.mensajes;
create policy msg_delete_admin
  on public.mensajes for delete
  to authenticated
  using (public.pawpals_es_administrador());

-- --- reportes ---
drop policy if exists rep_select_admin on public.reportes;
create policy rep_select_admin
  on public.reportes for select
  to authenticated
  using (public.pawpals_es_administrador());

drop policy if exists rep_insert_reportante on public.reportes;
create policy rep_insert_reportante
  on public.reportes for insert
  to authenticated
  with check ((auth.uid())::text = uid_reportante);

drop policy if exists rep_update_admin on public.reportes;
create policy rep_update_admin
  on public.reportes for update
  to authenticated
  using (public.pawpals_es_administrador())
  with check (public.pawpals_es_administrador());

drop policy if exists rep_delete_admin on public.reportes;
create policy rep_delete_admin
  on public.reportes for delete
  to authenticated
  using (public.pawpals_es_administrador());

-- -----------------------------------------------------------------------------
-- Storage (fotos: usuarios/{uid}/perfil.jpg y perros/{uidDueno}/perro.jpg)
-- -----------------------------------------------------------------------------

insert into storage.buckets (id, name, public)
values ('medios', 'medios', true)
on conflict (id) do nothing;

-- Lectura pública de objetos en bucket medios (equivalente a URLs públicas de Firebase).
drop policy if exists medios_read_public on storage.objects;
create policy medios_read_public
  on storage.objects for select
  to public
  using (bucket_id = 'medios');

-- Subida: solo rutas usuarios/{mi_uid}/… o perros/{mi_uid}/…
drop policy if exists medios_insert_authenticated_own on storage.objects;
create policy medios_insert_authenticated_own
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'medios'
    and (
      public.pawpals_es_administrador()
      or (
        name like 'usuarios/' || (auth.uid())::text || '/%'
      )
      or (
        name like 'perros/' || (auth.uid())::text || '/%'
      )
    )
  );

drop policy if exists medios_update_authenticated_own on storage.objects;
create policy medios_update_authenticated_own
  on storage.objects for update
  to authenticated
  using (
    bucket_id = 'medios'
    and (
      public.pawpals_es_administrador()
      or name like 'usuarios/' || (auth.uid())::text || '/%'
      or name like 'perros/' || (auth.uid())::text || '/%'
    )
  )
  with check (
    bucket_id = 'medios'
    and (
      public.pawpals_es_administrador()
      or name like 'usuarios/' || (auth.uid())::text || '/%'
      or name like 'perros/' || (auth.uid())::text || '/%'
    )
  );

drop policy if exists medios_delete_authenticated_own on storage.objects;
create policy medios_delete_authenticated_own
  on storage.objects for delete
  to authenticated
  using (
    bucket_id = 'medios'
    and (
      public.pawpals_es_administrador()
      or name like 'usuarios/' || (auth.uid())::text || '/%'
      or name like 'perros/' || (auth.uid())::text || '/%'
    )
  );

-- -----------------------------------------------------------------------------
-- Realtime (chat): opcional; quita el comentario si usas suscripciones Realtime.
-- -----------------------------------------------------------------------------
-- alter publication supabase_realtime add table public.mensajes;
-- alter publication supabase_realtime add table public.conversaciones;

-- =============================================================================
-- Post-instalación manual recomendada
-- =============================================================================
-- 1) Crea un usuario admin en Authentication y luego inserta/actualiza su fila
--    en public.usuarios con rol = 'administrador' (mismo id que auth.uid()).
-- 2) Si el primer arranque de la app hace "upsert" de perfil tras login,
--    asegúrate de insertar la fila en `usuarios` con id = auth.uid()::text.
-- 3) Rotación: la clave "publishable" que compartiste en chat debería tratarse
--    como sensible; si se filtró, genera otra en el panel de Supabase.
-- 4) Ejecuta también `pawpals_rpc_eliminar_cuenta.sql` para el borrado de cuenta
--    desde la app (RPC `eliminar_cuenta_auth`).
-- =============================================================================
