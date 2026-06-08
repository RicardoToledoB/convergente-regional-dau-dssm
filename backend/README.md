# Backend - Convergente Regional DAU DSSM v2.1 CORREGIDO

## Requisitos

- Java 17+
- Maven 3.9+
- MySQL 8+

## Base de datos esperada

Esta version ya viene con `application.properties` configurado para:

```text
Base de datos: convergente_dau_dssm_db
Usuario:       convergente_dau_user
Password:      ConvergenteDau2026*
Host:          localhost
Puerto:        3306
Backend:       8086
```

Crear base de datos manualmente:

```sql
CREATE DATABASE IF NOT EXISTS convergente_dau_dssm_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'convergente_dau_user'@'localhost'
IDENTIFIED BY 'ConvergenteDau2026*';

GRANT ALL PRIVILEGES ON convergente_dau_dssm_db.*
TO 'convergente_dau_user'@'localhost';

FLUSH PRIVILEGES;
```

O ejecutar:

```bash
mysql -u root -p < ../database/crear_base_datos.sql
```

## Archivo de configuracion

El archivo activo es:

```text
src/main/resources/application.properties
```

Ya no se usa `application.yml` en esta version para evitar confusion.

## Correccion Row size too large

Se corrigio `DauAttentionEntity` para evitar el error de MySQL por exceso de columnas `varchar(255)`. Los campos largos quedaron como `TEXT`:

- `motivoConsulta`
- `hipotesisDiagnostico`
- `indicacionFarmacos`
- `solicitudMediosDiagnostico`
- `descripcionMediosDiagnostico`
- `diagnosticoFinal`

Y el payload original sigue como `LONGTEXT` en `DauEventEntity`.

## Ejecutar

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:8086/swagger-ui.html
```

## Recepcion DAU

```bash
curl -X POST http://localhost:8086/api/integration/dau/eventos \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: CAMBIAR_API_KEY_HCM" \
  -H "X-FILENAME: 204907_01_Admision_2026_06_05_08_30_55.json" \
  -d @../examples/json/204907_01_Admision_2026_06_05_08_30_55.json
```

## Reglas implementadas

- El contrato externo conserva nombres originales del JSON, incluyendo `fechaAdminision`, `codigoDiagnistico` y `tituloProfosionalPrimeraCategorizacion`.
- Se consolida por `idDAU + idAtencion`.
- Se guarda cada evento con payload original y hash SHA-256.
- No se sobrescriben datos existentes con `null`.
- El tipo de evento se infiere por `X-FILENAME` o por campos presentes.
