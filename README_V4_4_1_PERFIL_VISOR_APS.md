# v4.4.1 - Perfil exclusivo VISOR_APS para Pantalla APS

## Objetivo
Se agrega un perfil exclusivo para que un usuario solo pueda acceder al módulo **Pantalla APS**. El usuario administrador mantiene acceso completo a todos los módulos.

## Rol nuevo
- `VISOR_APS`

## Comportamiento esperado
- `ADMIN`: ve todo el sistema.
- `VISOR_APS`: solo ve el menú y la vista **Pantalla APS**.
- Otros roles mantienen el comportamiento de la versión anterior, sin acceso al módulo exclusivo de pantalla APS.

## Backend
- Se agrega `VISOR_APS` al enum `Role`.
- El endpoint `GET /api/pantallas/aps` deja de ser público.
- El endpoint queda restringido a:
  - `ADMIN`
  - `VISOR_APS`

## Frontend
- Se agrega `VISOR_APS` en el selector de roles de administración de usuarios.
- Al iniciar sesión con `VISOR_APS`, el sistema entra directamente a **Pantalla APS**.
- El menú lateral oculta Dashboard, Monitor DAU, Eventos, Errores, Usuarios y Gestión Red para `VISOR_APS`.

## Usuario sugerido para crear desde Administración de usuarios

- Username: `visor_aps`
- Rol: `VISOR_APS`
- Nombre completo: `Visor Pantalla APS`
- Proveedor/origen: `DSSM`

Por seguridad, definir una clave institucional propia al crear el usuario.
