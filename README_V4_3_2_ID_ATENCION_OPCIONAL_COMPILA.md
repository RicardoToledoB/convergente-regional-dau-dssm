# v4.3.2 - idAtencion opcional

Ajuste aplicado para cumplir observación de interoperabilidad:

- `idDAU` sigue siendo obligatorio.
- `idAtencion` deja de ser obligatorio.
- Si `idAtencion` llega `null`, vacío (`""`) o con espacios, el backend lo normaliza internamente usando el valor de `idDAU`.
- Se mantiene la consolidación actual por `idDAU + idAtencion`, usando `idDAU + idDAU` cuando `idAtencion` no sea informado.
- Se corrige el error de compilación Java: `local variables referenced from a lambda expression must be final or effectively final`.

## Archivo modificado

`backend/src/main/java/cl/dssm/dau/service/DauIngestionService.java`

## Compilar

```bash
cd backend
mvn clean package -DskipTests
```

## Reiniciar servicio en servidor

```bash
sudo systemctl restart convergente-dau-api
sudo systemctl status convergente-dau-api
```

## Probar caso idAtencion vacío

```bash
TOKEN=$(curl -sk https://convergente-api.dssm.cl/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | grep -o '"token":"[^"]*' | cut -d':' -f2- | tr -d '"')

curl -k -X POST https://convergente-api.dssm.cl/api/integration/dau/eventos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-FILENAME: prueba_id_atencion_vacio.json" \
  -d @../examples/json/00_PRUEBA_ID_ATENCION_VACIO.json
```

Respuesta esperada: `ok: true`, con `idAtencion` normalizado al valor de `idDAU`.
