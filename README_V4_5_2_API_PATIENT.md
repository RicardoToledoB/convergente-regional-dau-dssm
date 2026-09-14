# V4.5.2 - API de eventos por paciente

Se incorpora el endpoint solicitado:

```http
GET /api/patient/{rut}
```

URL pública esperada a través del vhost frontend:

```text
https://convergente.dssm.cl/api/patient/{rut}
```

## Comportamiento

- Busca al paciente por `run` y, si viene informado, también por `dv` en `dau_atenciones_consolidadas`.
- Acepta RUT completo con puntos/guion o sin formato; exige dígito verificador válido.
- Recupera todos los `idDAU` asociados al paciente.
- Devuelve todos los eventos históricos de esos DAU desde `dau_eventos_recibidos`.
- Orden cronológico ascendente por `fechaRecepcion`.
- Si no existen eventos devuelve `[]` con HTTP 200.
- No modifica ni consolida datos.
- No expone metadatos internos como IP de origen, API key o hash del payload.

## Respuesta

La respuesta es directamente un arreglo JSON:

```json
[
  {
    "id": 21872,
    "idDau": "63240647",
    "idAtencion": null,
    "tipoEvento": "01_ADMISION",
    "fechaRecepcion": "2026-09-11T11:20:24.797988",
    "estadoProcesamiento": "PROCESADO",
    "payload": {
      "idDAU": "63240647",
      "run": "6617126",
      "dv": "4",
      "idAtencion": null
    }
  }
]
```

## Seguridad

Por tratarse de información clínica asociada a un RUT, el endpoint **no queda público/anónimo**.

Permite:

- JWT con rol `ADMIN`, o
- cabecera `X-API-KEY` con la clave configurada en la variable de entorno `PATIENT_API_KEY`.

Ejemplo:

```bash
curl -s \
  -H "X-API-KEY: $PATIENT_API_KEY" \
  "https://convergente.dssm.cl/api/patient/6617126-4"
```

## Apache

El backend corre en `8086`, mientras `convergente.dssm.cl` sirve Angular. Para que la URL solicitada funcione exactamente en ese dominio se debe agregar al vhost HTTPS:

```apache
ProxyPreserveHost On
ProxyPass        /api/patient/ http://127.0.0.1:8086/api/patient/
ProxyPassReverse /api/patient/ http://127.0.0.1:8086/api/patient/
```

Si existe fallback SPA por `mod_rewrite`, excluir `/api/patient/` antes de enviar solicitudes a `index.html`.

## Archivos agregados/modificados

- `controller/PatientController.java`
- `service/PatientEventService.java`
- `dto/PatientEventResponse.java`
- `repository/DauAttentionRepository.java`
- `repository/DauEventRepository.java`
- `security/ApiKeyFilter.java`
- `config/SecurityConfig.java`
- `application.properties`
- `docs/apache-patient-api.conf`

## Rendimiento

Se agregó el índice JPA `idx_atencion_run_dv (run,dv)`. Para ambientes con `JPA_DDL_AUTO=validate`, se incluye `database/v4.5.2_indice_rut_patient_api.sql` para crearlo manualmente una sola vez.
