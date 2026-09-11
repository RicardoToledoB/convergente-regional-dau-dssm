# v4.4.3 - Consolidación canónica segura RAYEN

## Objetivo
Evitar que un mismo DAU quede fragmentado entre una atención temporal `idAtencion=idDAU` y una atención real posterior.

## Correcciones
- `idDAU` sigue siendo obligatorio e `idAtencion` es opcional.
- Si `idAtencion` llega vacío, sólo la tabla consolidada usa temporalmente `idAtencion=idDAU`.
- La bitácora `dau_eventos_recibidos` conserva el `idAtencion` recibido originalmente y el payload original; no se relinkan ni reescriben eventos históricos.
- Cuando aparece el `idAtencion` real para el mismo `idDAU`:
  - se adopta como identificador canónico;
  - se fusionan los datos faltantes del registro temporal;
  - se conserva el estado clínico más avanzado;
  - se elimina únicamente la fila temporal de `dau_atenciones_consolidadas`.
- Los reenvíos duplicados vuelven a ejecutar la consolidación de forma idempotente sin duplicar la bitácora.
- El estado nunca retrocede: `ADMISION < CATEGORIZADA < ATENCION_MEDICA < ALTA_MEDICA`.
- Se reconoce atención médica por datos clínicos aunque `fechaAtencion`/`horaAtencion` vengan vacíos.
- Se aceptan ambos nombres de diagnóstico: `codigoDiagnistico` y `codigoDiagnostico`.

## Validación
```bash
cd backend
mvn clean package -DskipTests
```

Caso esperado:
1. llega `idDAU=X`, `idAtencion=null` -> consolidado temporal `X/X`;
2. llega `idDAU=X`, `idAtencion=Y` -> queda una sola fila consolidada `X/Y`;
3. la bitácora mantiene ambos eventos tal como llegaron.
