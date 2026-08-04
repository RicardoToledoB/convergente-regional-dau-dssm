# v4.3.1 - Hotfix idAtencion opcional

## Cambio aplicado

Se ajusta la recepción de eventos DAU para cumplir con la observación de interoperabilidad:

- `idDAU` se mantiene como campo obligatorio.
- `idAtencion` deja de ser obligatorio.
- Cuando `idAtencion` llega `null`, vacío (`""`) o solo con espacios, el backend lo normaliza internamente usando el mismo valor de `idDAU`.

Esto permite mantener la consolidación actual basada en la llave interna `idDAU + idAtencion`, sin rechazar eventos que cumplan el normativo convergente donde `idAtencion` no es obligatorio.

## Archivo modificado

- `backend/src/main/java/cl/dssm/dau/service/DauIngestionService.java`

## Ejemplo agregado

- `examples/json/00_PRUEBA_ID_ATENCION_VACIO.json`

## Respuesta esperada

Si se envía un evento con:

```json
{
  "idDAU": "990001",
  "idAtencion": ""
}
```

El servicio debe responder exitosamente, normalizando internamente:

```json
{
  "ok": true,
  "message": "Evento DAU recibido",
  "data": {
    "idDau": "990001",
    "idAtencion": "990001",
    "resultado": "PROCESADO"
  }
}
```

## Comandos sugeridos para validar

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

En otra terminal:

```bash
TOKEN=$(curl -s -X POST http://localhost:8086/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | grep -o '"token":"[^"]*' | cut -d':' -f2 | tr -d '"')

curl -X POST http://localhost:8086/api/integration/dau/eventos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-FILENAME: prueba_id_atencion_vacio.json" \
  --data-binary @examples/json/00_PRUEBA_ID_ATENCION_VACIO.json
```
