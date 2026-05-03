# Migracion de Datos a Espanol

Este proyecto mantiene la marca `PawPals` y el identificador Android `com.pawpals.app`, pero los contratos propios de datos pasan a estar en espanol.

## Colecciones Firestore

- `users` -> `usuarios`
- `dogs` -> `perros`
- `matches` -> `coincidencias`
- `chats` -> `conversaciones`
- `messages` -> `mensajes`
- `reports` -> `reportes`
- `friendRequests` -> `solicitudesAmistad`
- `friends` -> `amigos`
- `supportTickets` -> `ticketsSoporte`
- `swipes` -> `deslizamientos`

## Campos Principales

- `displayName` -> `nombreVisible`
- `ownerUid` -> `uidDueno`
- `photoUrl` -> `urlFoto`
- `createdAt` -> `creadoEn`
- `updatedAt` -> `actualizadoEn`
- `senderUid` -> `uidRemitente`
- `targetType` -> `tipoObjetivo`
- `targetId` -> `idObjetivo`
- `reporterUid` -> `uidReportante`
- `participants` -> `participantes`
- `userLow` -> `usuarioMenor`
- `userHigh` -> `usuarioMayor`
- `friendsCount` -> `numeroAmigos`
- `walksCount` -> `numeroPaseos`
- `matchesCount` -> `numeroCoincidencias`

## Preferencias Locales

- `pawpals_preferences` -> `pawpals_preferencias`
- `onboarding_completed` -> `bienvenida_completada`
- `notify_messages` -> `notificar_mensajes`
- `notify_walks` -> `notificar_paseos`
- `notify_matches` -> `notificar_coincidencias`
- `location_only_while_walking` -> `ubicacion_solo_durante_paseo`

Los datos existentes en Firebase con nombres antiguos no se leeran tras este cambio si no se migran o recrean con estas claves nuevas.
