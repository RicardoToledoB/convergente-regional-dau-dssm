# v4.3.3 - Hotfix RAYEN: idAtencion canónico e inferencia de atención médica

## Problema detectado

RAYEN envió el flujo DAU con este comportamiento:

1. **Admisión** con `idAtencion = null`.
2. **Categorización / atención / alta** con `idAtencion` real, por ejemplo `700241257`.
3. El evento de atención médica puede venir con `fechaAtencion` y `horaAtencion` nulos, pero con `codigoDiagnistico` informado.

La versión anterior aceptaba `idAtencion` vacío, pero lo normalizaba como `idAtencion = idDAU`. Cuando después llegaba el `idAtencion` real, se podía fragmentar la consolidación en dos atenciones:

- `idDAU = 59452384`, `idAtencion = 59452384`
- `idDAU = 59452384`, `idAtencion = 700241257`

Además, el evento de atención médica sin fecha/hora podía inferirse como categorización si venían también los campos de categorización.

## Correcciones aplicadas

### 1. idAtencion canónico

- Si llega `idAtencion` vacío, se sigue aceptando y se usa `idDAU` como identificador temporal.
- Si posteriormente llega el mismo `idDAU` con un `idAtencion` real, el sistema adopta ese `idAtencion` como canónico.
- Si existe una atención temporal `idAtencion = idDAU`, se fusiona con la atención canónica.
- Los eventos históricos temporales se relinkan desde `idAtencion = idDAU` hacia el `idAtencion` real.

### 2. Inferencia correcta de atención médica

Ahora se infiere `03_ATENCION_MEDICA` cuando existan antecedentes clínicos o diagnósticos, aunque `fechaAtencion` y `horaAtencion` vengan nulos:

- `codigoDiagnistico`
- `codigoDiagnostico`
- `hipotesisDiagnostico`
- `tipoCodigoDiagnostico`
- `indicacionFarmacos`
- `idReceta`
- `solicitudMediosDiagnostico`
- `descripcionMediosDiagnostico`

La prioridad de inferencia queda así:

1. Alta médica
2. Atención médica
3. Categorización
4. Admisión

## Archivos modificados

- `backend/src/main/java/cl/dssm/dau/service/DauIngestionService.java`
- `backend/src/main/java/cl/dssm/dau/repository/DauEventRepository.java`

## Validación recomendada

Compilar:

```bash
cd backend
mvn clean package -DskipTests
```

Reiniciar servicio:

```bash
sudo systemctl restart convergente-dau-api
sudo systemctl status convergente-dau-api
```

Validar en base de datos:

```sql
SELECT 
  id_dau,
  id_atencion,
  estado_actual,
  fecha_adminision,
  hora_admision,
  primera_categorizacion,
  codigo_diagnistico,
  fecha_alta,
  hora_alta
FROM dau_atenciones_consolidadas
WHERE id_dau = '59452384';
```

Resultado esperado: una sola atención consolidada bajo el `idAtencion` real, por ejemplo:

```text
59452384 | 700241257 | ALTA_MEDICA
```

Validar eventos:

```sql
SELECT 
  id_dau,
  id_atencion,
  tipo_evento_inferido,
  estado_procesamiento,
  fecha_recepcion
FROM dau_eventos_recibidos
WHERE id_dau = '59452384'
ORDER BY fecha_recepcion;
```

Resultado esperado:

```text
01_ADMISION
02_CATEGORIZACION
03_ATENCION_MEDICA
04_ALTA_MEDICA
```

## Nota sobre eventos ya recibidos

Si el caso `59452384` ya fue recibido antes del hotfix y quedó fragmentado, basta con reenviar cualquiera de los eventos posteriores que ya traen `idAtencion = 700241257`. Aunque el sistema lo marque como `DUPLICADO`, ejecutará nuevamente la consolidación idempotente y fusionará la atención temporal con la canónica.
