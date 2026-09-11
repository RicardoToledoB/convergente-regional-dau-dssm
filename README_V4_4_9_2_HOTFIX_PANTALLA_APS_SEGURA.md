# V4.4.9.2 – Hotfix Pantalla APS segura

- Mantiene la consolidación estricta por contrato JSON de V4.4.9/4.4.9.1.
- Reincorpora sólo en la visualización APS una ventana de seguridad de 24 horas desde `fechaUltimoEvento`.
- No cambia `estado_actual`.
- No genera altas artificiales.
- No modifica `dau_eventos_recibidos`.
- Evita que DAU históricos sin cierre transmitido inflen la pantalla operacional.
