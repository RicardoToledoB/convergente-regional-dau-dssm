# V4.4.5.1 - Hotfix selector de establecimientos Pantalla APS

Corrección del selector dinámico de establecimientos en Pantalla APS.

## Cambios
- El frontend consulta el catálogo desde `/api/pantallas/aps/establecimientos`, endpoint protegido específicamente para ADMIN y VISOR_APS.
- Se normaliza `codigo` a número para compatibilidad con `mat-select`.
- Se agrega respaldo automático usando `distribucionPorEstablecimiento` de la vista "Red completa" si el catálogo no responde o llega vacío.
- Los establecimientos detectados se sincronizan también con el catálogo general usado por el resto del frontend.
- No modifica datos clínicos, estados, eventos ni lógica de consolidación.
