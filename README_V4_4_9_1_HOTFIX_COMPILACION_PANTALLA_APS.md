# V4.4.9.1 – Hotfix compilación Pantalla APS

Corrige el uso de `notBlank(...)` en `PantallaApsService`, método que no existe en esa clase.
Se reutiliza el helper existente `trimToNull(...)` para validar fecha/hora de alta y atención.
No cambia la lógica funcional de V4.4.9.
