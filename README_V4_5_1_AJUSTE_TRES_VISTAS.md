# V4.5.1 – Ajuste de las tres vistas del Visor Integrado de Urgencia

Corrección funcional sobre V4.5.0 para representar de forma separada los requerimientos de las diapositivas 2, 3 y 4 del proyecto.

## Cambios

### Perfil 1 · Vista 1 – Mi urgencia
- Mantiene logo parametrizable y nombre del establecimiento.
- Muestra únicamente:
  - Pacientes en espera.
  - Pacientes en atención.
  - Tiempo promedio de espera.
  - Tiempo de espera por categorización C1–C5 y S/C.
- Se elimina el listado individual de pacientes de la pantalla pública.
- Se elimina el KPI de tiempo máximo de espera de esta vista para ajustarse al requerimiento.
- Se eliminan los gráficos secundarios de la parte inferior en esta vista pública.

### Perfil 1 · Vista 2 – Red de mi comuna
- Nueva vista diferenciada de la vista de gestión.
- Presenta los **otros establecimientos de urgencia de la misma comuna** del centro asociado.
- Excluye el establecimiento que está mostrando la pantalla.
- Columnas: Establecimiento, C1, C2, C3, C4, C5, Espera, Atención y Promedio.
- No muestra KPI de gestión.
- En el perfil `VISOR_APS`, rota automáticamente con Vista 1 cada 30 segundos.

### Perfil 2 · Gestión comunal/regional
- Se mantiene como una tercera vista independiente.
- Gestor comunal: restringido a su comuna en backend.
- Gestor regional: puede visualizar todas las comunas o seleccionar una comuna.
- La tabla se muestra primero y los KPI consolidados debajo, según el requerimiento.
- KPI: Total en espera, En atención, Promedio comunal/regional de espera y Centros activos.
- Columnas principales: Establecimiento, C1, C2, C3, C4, C5, Espera, Atención y Promedio. En alcance regional se agrega Comuna.

## Modos internos frontend

- `centro`: Perfil 1 / Vista 1.
- `comuna`: Perfil 1 / Vista 2.
- `gestion`: Perfil 2.

## Seguridad y datos

No se modifica la lógica de ingestión, consolidación, historial ni estados clínicos. Se mantienen las restricciones de `VISOR_APS`, `GESTOR_COMUNAL` y `GESTOR_REGIONAL` implementadas en V4.5.0.

## Validación realizada

Se realizó validación sintáctica TypeScript mediante `typescript.transpileModule` sin diagnósticos. La compilación Angular completa debe ejecutarse en el servidor DSSM con las dependencias instaladas.
