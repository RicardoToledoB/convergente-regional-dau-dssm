# V4.5.3.2 – Red comunal incluye el centro actual

Ajuste solicitado para Perfil 1 / Sala de Espera.

## Cambio
La Vista 2 "Red de urgencia" ahora muestra **todos los establecimientos de urgencia de la comuna**, incluyendo el propio establecimiento cuya pantalla está realizando la rotación.

Ejemplo para Punta Arenas, si la pantalla está asociada a SAR Juan Damianovic (126801), la Vista 2 debe incluir:
- 126100 - Hospital Clínico Magallanes
- 126800 - SAPU Dr. Mateo Bencur
- 126801 - SAR Juan Damianovic
- 126900 - SAPU 18 de Septiembre
- 201069 - SAPU Carlos Ibáñez

La Vista 1 continúa mostrando exclusivamente el detalle del centro seleccionado. La Vista 2 representa la red comunal completa.

## Frontend
- Se eliminó el filtro que excluía `codigoActual` de `pantallaCentrosComunaSala`.
- El contador cambia de "otros establecimientos" a "establecimientos".
- Se ajustó el subtítulo y el mensaje de lista vacía.

No se modifica backend, ingestión, consolidación ni Perfil 2 Gestión.
