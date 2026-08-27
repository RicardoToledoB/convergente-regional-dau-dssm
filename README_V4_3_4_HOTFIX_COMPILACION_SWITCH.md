# v4.3.4 Hotfix compilación switch DauEstado

Corrección menor sobre v4.3.3.

## Problema corregido

Maven fallaba al compilar con:

```text
the switch expression does not cover all possible input values
```

en `DauIngestionService.java`, método `rank(DauEstado estado)`.

## Causa

El enum `DauEstado` incluye `ERROR`, pero el `switch` solo cubría:

- `ADMISION`
- `CATEGORIZADA`
- `ATENCION_MEDICA`
- `ALTA_MEDICA`

## Solución

Se agregó:

```java
case ERROR -> 0;
```

Con esto el `switch` cubre todos los valores del enum y compila correctamente.

## Mantiene

- idAtencion opcional.
- idAtencion canónico para RAYEN.
- Fusión de atención temporal `idAtencion = idDAU` con atención real.
- Relink de eventos previos temporales al idAtencion real.
- Inferencia de atención médica por `codigoDiagnistico`/`codigoDiagnostico` aunque fecha/hora atención vengan null.
