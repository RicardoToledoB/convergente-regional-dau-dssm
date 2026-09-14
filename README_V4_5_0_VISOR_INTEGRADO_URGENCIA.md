# V4.5.0 – Visor Integrado de Urgencia

Extensión funcional sobre V4.4.9.2 para incorporar los requerimientos de las diapositivas 2 a 4 del proyecto "Visor Integrado de Urgencia".

## Incorporado

- **Perfil Sala de Espera (`VISOR_APS`)**
  - Vista del establecimiento asociado al usuario.
  - Logo parametrizable mediante `logoUrl`.
  - Nombre del establecimiento, pacientes en espera, pacientes en atención, promedio/máximo y tiempos C1–C5/S/C.
  - Segunda vista **Red de urgencia de la comuna**.
  - Rotación automática entre "Mi urgencia" y "Red de urgencia" cada 30 segundos (sobre refresco de 15 segundos).

- **Gestión Comunal (`GESTOR_COMUNAL`)**
  - Acceso directo al Visor Integrado.
  - Alcance restringido en backend a la `comuna` asociada al usuario.
  - KPI: total en espera, en atención, promedio de espera y centros activos.
  - Tabla comparativa por establecimiento con C1–C5, S/C, espera, atención y promedio.

- **Gestión Regional (`GESTOR_REGIONAL`)**
  - Acceso directo al Visor Integrado.
  - Puede visualizar todas las comunas o seleccionar una comuna.
  - Tabla comparativa regional/comunal y KPI consolidados.

- **Parametrización de usuarios**
  - Nuevos campos en `users`: `comuna`, `establecimiento_codigo`, `logo_url`.
  - Se agregan a alta/edición de usuarios y respuesta de autenticación.
  - `spring.jpa.hibernate.ddl-auto=update` crea las columnas al iniciar con el esquema actual.

## Nuevos endpoints

- `GET /api/pantallas/aps/comunas`
- `GET /api/pantallas/aps/red?comuna=Punta%20Arenas`

Los endpoints aplican el alcance del usuario en backend; un `GESTOR_COMUNAL` no puede ampliar su alcance modificando manualmente el parámetro `comuna`, y un `VISOR_APS` con establecimiento configurado queda restringido a ese establecimiento/comuna.

## Comunas configuradas inicialmente

- Punta Arenas: 126801, 126800, 126900, 201069, 126100
- Puerto Natales: 201079, 126101, 121105
- Porvenir: 126102, 121110, 121102
- Cabo de Hornos: 126704, 121120, 121108

Los códigos no incluidos quedan bajo `Otra comuna` hasta parametrizarlos.

## Importante

Esta versión **no altera** `dau_eventos_recibidos`, no genera altas artificiales y no modifica la consolidación clínica estricta incorporada en V4.4.9. Mantiene la ventana operacional de seguridad de V4.4.9.2 para la visualización actual.

## Validación

El entorno de construcción usado para generar este paquete no dispone de Maven y no tuvo `node_modules` disponible para ejecutar Angular CLI. Por ello se debe validar compilación en el servidor DSSM antes de reemplazar producción.
