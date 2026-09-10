# v4.4.2.1 - Hotfix compilación Pantalla APS

Corrige el error de compilación de `PantallaApsService` causado por la referencia al método faltante `tramoHorario(DauAttentionEntity)`.

La función clasifica la hora de admisión en los tramos:

- 00 a 08
- 08 a 12
- 12 a 16
- 16 a 20
- 20 a 24

Si no existe una hora válida, retorna `S/D` y la interfaz muestra `Sin dato`.
