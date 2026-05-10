-- script principal para montar la base de datos de pawpals en supabase
-- se pega entero en el editor sql de supabase y se ejecuta una vez
-- las claves y urls no van aqui, se guardan en local.properties
-- lo del correo de confirmacion se cambia desde el panel de auth, no con sql

-- pgcrypto se usa para crear uuid cuando hace falta
create extension if not exists "pgcrypto";

-- primero van las tablas y despues las funciones que las consultan

-- tablas de la app

create table if not exists public.usuarios (
  id text primary key, -- auth.uid()::text o id de prueba
  correo text not null default '',
  nombre_visible text not null default '',
  zona text not null default '',
  sobre_mi text not null default '',
  rol text not null default 'usuario', -- 'usuario' | 'administrador'
  bloqueado boolean not null default false,
  latitud double precision,
  longitud double precision,
  ubicacion_actualizada_en bigint,
  token_fcm text,
  url_foto text,
  numero_amigos integer not null default 0,
  numero_paseos integer not null default 0,
  numero_coincidencias integer not null default 0,
  paseando boolean not null default false,
  creado_en bigint,
  es_demo boolean not null default false -- app: demo
);

create index if not exists idx_usuarios_es_demo on public.usuarios (es_demo) where es_demo = true;

create table if not exists public.perros (
  id text primary key,
  uid_dueno text not null references public.usuarios (id) on delete cascade,
  nombre text not null default '',
  raza text not null default '',
  edad_anios integer not null default 1,
  biografia text not null default '',
  url_foto text,
  energia text not null default 'moderado', -- app: energia
  sociabilidad text not null default 'muy_sociable', -- app: sociabilidad
  actualizado_en bigint,
  es_demo boolean not null default false -- app: demo
);

create index if not exists idx_perros_uid_dueno on public.perros (uid_dueno);

create table if not exists public.coincidencias (
  id text primary key,
  usuario_a text not null,
  usuario_b text not null,
  usuario_menor text not null,
  usuario_mayor text not null,
  participantes text[] not null, -- exactamente dos ids
  iniciador text not null,
  estado text not null default 'pendiente', -- pendiente | aceptada | rechazada
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_coincidencias_participantes on public.coincidencias using gin (participantes);
create index if not exists idx_coincidencias_menor_mayor on public.coincidencias (usuario_menor, usuario_mayor);

create table if not exists public.deslizamientos (
  id text primary key,
  uid_origen text not null,
  uid_destino text not null,
  accion text not null, -- me_gusta | super_me_gusta | descartar
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_desliz_origen on public.deslizamientos (uid_origen);

create table if not exists public.solicitudes_amistad (
  id text primary key,
  uid_origen text not null,
  uid_destino text not null,
  estado text not null default 'pendiente', -- pendiente | aceptada | rechazada
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_solicitudes_destino on public.solicitudes_amistad (uid_destino, estado);

-- una fila por cada amistad guardada en los dos sentidos
create table if not exists public.amigos (
  usuario_id text not null references public.usuarios (id) on delete cascade,
  amigo_id text not null references public.usuarios (id) on delete cascade,
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint,
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

-- datos generales del chat; los mensajes van en otra tabla
create table if not exists public.conversaciones (
  id text primary key,
  participantes text[] not null,
  creado_en bigint,
  ultimo_mensaje text,
  ultimo_mensaje_en bigint,
  ultimo_remitente text
);

create table if not exists public.mensajes (
  id uuid primary key default gen_random_uuid(),
  conversacion_id text not null references public.conversaciones (id) on delete cascade,
  uid_remitente text not null,
  texto text not null default '',
  marca_temporal bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_mensajes_conversacion_tiempo on public.mensajes (conversacion_id, marca_temporal);

create table if not exists public.reportes (
  id uuid primary key default gen_random_uuid(),
  tipo_objetivo text not null,
  id_objetivo text not null,
  uid_reportante text not null,
  motivo text not null,
  estado text not null default 'abierto', -- abierto | revisado | accion_tomada
  creado_en bigint not null default (floor(extract(epoch from now()) * 1000))::bigint
);

create index if not exists idx_reportes_creado on public.reportes (creado_en desc);

-- funciones pequenas para no repetir las mismas comprobaciones en cada regla

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

-- acepto tambien el texto antiguo por si quedo algun dato viejo
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
      and u.rol = 'administrador'
  );
$$;

-- comprueba que el usuario esta dentro de esa conversacion
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

comment on function public.pawpals_es_administrador() is 'devuelve true si el usuario es administrador';

-- permisos de lectura y escritura

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

-- usuarios
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

-- perros
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

-- coincidencias
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

-- deslizamientos
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

-- solicitudes de amistad
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

-- amigos
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

-- mensajes de soporte
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

-- conversaciones
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

-- mensajes
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

-- reportes
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

-- bucket para las fotos de usuario y perro

insert into storage.buckets (id, name, public)
values ('medios', 'medios', true)
on conflict (id) do nothing;

-- las fotos se pueden leer para mostrarlas en la app
drop policy if exists medios_read_public on storage.objects;
create policy medios_read_public
  on storage.objects for select
  to public
  using (bucket_id = 'medios');

-- cada usuario solo puede subir en su propia carpeta
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

-- si se activa realtime para el chat, estas lineas se pueden descomentar
-- alter publication supabase_realtime add table public.mensajes;
-- alter publication supabase_realtime add table public.conversaciones;
