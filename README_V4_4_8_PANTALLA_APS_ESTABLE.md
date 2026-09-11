# V4.4.8 – Pantalla APS operacional estable

- Excluye ALTA_MEDICA, ERROR y registros con fechaAlta.
- ADMISION sólo permanece visible mientras idAtencion == idDau.
- CATEGORIZADA y ATENCION_MEDICA siguen el último estado consolidado recibido.
- Ventana máxima de seguridad: 24 horas desde fecha_ultimo_evento (fallback: admisión).
- No modifica ni elimina datos clínicos y no genera altas artificiales.
- En C1, C2, C3, C4, C5 y S/C se muestra debajo de HH:MM la cantidad de pacientes actualmente EN ESPERA en esa categoría.
- El conteo por categorías excluye pacientes que ya están en atención, por lo que su suma corresponde al KPI Pacientes en espera.
