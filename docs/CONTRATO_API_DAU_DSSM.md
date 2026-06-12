# Contrato API - Convergente Regional DAU DSSM

## Endpoint oficial

```http
POST /api/integration/dau/eventos
```

## Seguridad

Headers obligatorios:

```http
Content-Type: application/json
X-API-KEY: <API_KEY_ENTREGADA_POR_DSSM>
```

Header recomendado:

```http
X-FILENAME: 204907_01_Admision_2026_06_05_08_30_55.json
```

## Principio de integracion

Cada JSON representa un evento incremental de una atencion DAU. El sistema DSSM consolida por:

```text
idDAU + idAtencion
```

## Reglas

1. Se acepta la estructura plana actual del RCE.
2. Se mantienen nombres actuales por compatibilidad: `fechaAdminision`, `codigoDiagnistico`, `tituloProfosionalPrimeraCategorizacion`.
3. Se guarda el payload original completo.
4. Se actualiza una vista consolidada de atencion.
5. No se sobrescriben datos existentes con `null`.
6. Cada evento queda auditado con hash SHA-256, IP origen y fecha de recepcion.
7. El tipo de evento se infiere por nombre de archivo o por campos presentes.

## Respuesta esperada

```json
{
  "ok": true,
  "message": "Evento DAU recibido",
  "data": {
    "idDau": "204907",
    "idAtencion": "204907",
    "tipoEventoInferido": "01_ADMISION",
    "estadoAtencion": "ADMISION",
    "hashPayload": "...",
    "resultado": "PROCESADO"
  }
}
```
