# V4.5.2.1 – INTEGRADOR habilitado para API Patient

## Ajuste
Se habilita el rol `INTEGRADOR` para consultar:

`GET /api/patient/{rut}`

Esto permite que cuentas técnicas autenticadas como `portalpaciente_pre` obtengan un JWT por `/api/auth/login` y utilicen dicho token para consultar los eventos del paciente.

## Autorización
El endpoint acepta ahora cualquiera de estas autoridades:

- `ROLE_ADMIN`
- `ROLE_INTEGRADOR`
- `ROLE_PATIENT_API` (API key técnica dedicada)

No se modifica la autorización del endpoint de ingestión DAU ni la lógica de consolidación.

## Ejemplo

1. Autenticar la cuenta INTEGRADOR:

```bash
curl -s -X POST 'https://convergente-api.dssm.cl/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"portalpaciente_pre","password":"<PASSWORD>"}'
```

2. Usar el JWT retornado:

```bash
curl -s 'https://convergente.dssm.cl/api/patient/6617126-4' \
  -H 'Authorization: Bearer <JWT>'
```

También continúa disponible el mecanismo `X-API-KEY` dedicado a `/api/patient/**`.
