# V4.4.2 – Visor APS Operacional

## Cambios
- Pacientes activos limitados a una ventana operacional configurable de 24 horas.
- Excluye altas, errores y registros con fecha de alta.
- En espera: admisión/categorización sin atención médica.
- En atención: atención médica sin alta.
- El visor público ya no expone identificadores del paciente ni BOX inferido.
- VISOR_APS usa pantalla completa, sin menú ni barra administrativa.
- Refresco automático cada 15 segundos.
- Rotación automática de páginas cada 12 segundos.
- Indicador de última actualización y pérdida de conexión.

## Configuración
`pantalla.aps.max-active-hours=24`

La ventana evita que DAU antiguos que nunca recibieron un evento de cierre permanezcan indefinidamente en una TV operacional.
