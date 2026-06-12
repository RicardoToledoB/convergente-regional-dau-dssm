#!/bin/bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8086}"
USER="${DAU_USER:-admin}"
PASS="${DAU_PASS:-admin123}"

TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" | grep -o '"token":"[^"]*' | cut -d':' -f2 | tr -d '"')

if [ -z "$TOKEN" ]; then echo "No se pudo obtener token"; exit 1; fi

enviar_evento() {
  curl -s -X POST "$BASE_URL/api/integration/dau/eventos" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-FILENAME: seed_dau_prueba.json" \
    -d "$1" >/dev/null
}

crear_dau() {
  local ID="$1" EST="$2" FECHA="$3" HADM="$4" HCATEG="$5" HATENC="$6" HALTA="$7" SEXO="$8" CAT="$9" MOTIVO="${10}" DX="${11}" CODDX="${12}" DESTINO="${13}"

  enviar_evento "{\"nombreSolucion\":\"Registro Clínico Electrónico\",\"numeroProceso\":3,\"mesAtencion\":6,\"anoAtencion\":2026,\"codigoSS\":\"26\",\"codigoEstablecimiento\":$EST,\"idDAU\":\"$ID\",\"idBDPersonas\":\"CL$ID\",\"idPaciente\":\"PAC$ID\",\"run\":\"1$ID\",\"dv\":\"9\",\"tipoIdentificacion\":1,\"fechaNacimiento\":\"19850115\",\"codSexo\":\"$SEXO\",\"prevision\":\"1\",\"clasificacionBeneficiarioFonasa\":\"D\",\"idAtencion\":\"$ID\",\"fechaAdminision\":\"$FECHA\",\"horaAdmision\":\"$HADM\",\"unidadAtencion\":\"02\",\"motivoConsulta\":\"$MOTIVO\",\"medioLlegada\":5}"
  enviar_evento "{\"nombreSolucion\":\"Registro Clínico Electrónico\",\"numeroProceso\":3,\"mesAtencion\":6,\"anoAtencion\":2026,\"codigoSS\":\"26\",\"codigoEstablecimiento\":$EST,\"idDAU\":\"$ID\",\"idAtencion\":\"$ID\",\"fechaAdminision\":\"$FECHA\",\"horaAdmision\":\"$HADM\",\"categorizacionESI\":\"SI\",\"primeraCategorizacion\":\"$CAT\",\"fechaPrimeraCategorizacion\":\"$FECHA\",\"horaPrimeraCategorizacion\":\"$HCATEG\",\"tituloProfosionalPrimeraCategorizacion\":\"03\",\"ultimaCategorizacion\":\"$CAT\",\"fechaUltimaCategorizacion\":\"$FECHA\",\"horaUltimaCategorizacion\":\"$HCATEG\",\"profesionalUltimaCategorizacion\":\"03\",\"numCategorizacion\":\"01\"}"
  enviar_evento "{\"nombreSolucion\":\"Registro Clínico Electrónico\",\"numeroProceso\":3,\"mesAtencion\":6,\"anoAtencion\":2026,\"codigoSS\":\"26\",\"codigoEstablecimiento\":$EST,\"idDAU\":\"$ID\",\"idAtencion\":\"$ID\",\"fechaAdminision\":\"$FECHA\",\"horaAdmision\":\"$HADM\",\"fechaAtencion\":\"$FECHA\",\"horaAtencion\":\"$HATENC\",\"hipotesisDiagnostico\":\"$DX\",\"codigoDiagnistico\":\"$CODDX\",\"tipoCodigoDiagnostico\":\"01\",\"indicacionFarmacos\":\"SEGUN INDICACION MEDICA\",\"solicitudMediosDiagnostico\":\"SEGUN EVALUACION CLINICA\",\"descripcionMediosDiagnostico\":\"REGISTRO DE PRUEBA PARA GESTION\"}"
  enviar_evento "{\"nombreSolucion\":\"Registro Clínico Electrónico\",\"numeroProceso\":3,\"mesAtencion\":6,\"anoAtencion\":2026,\"codigoSS\":\"26\",\"codigoEstablecimiento\":$EST,\"idDAU\":\"$ID\",\"idAtencion\":\"$ID\",\"fechaAdminision\":\"$FECHA\",\"horaAdmision\":\"$HADM\",\"fechaAlta\":\"$FECHA\",\"horaAlta\":\"$HALTA\",\"diagnosticoFinal\":\"$DX\",\"tipoDiagnostico\":\"02\",\"codigoDiagnosticoAltaMedica\":\"$CODDX\",\"tipoCodDiagnosticoAltaMedica\":\"01\",\"condicionCierreAtencion\":\"01\",\"destinoAlta\":\"$DESTINO\",\"pertinencia\":\"SI\",\"idProfesionalAlta\":\"PROF$ID\",\"runProfesional\":\"17654321\",\"dvProfesional\":\"8\",\"tituloProfesional\":\"01\",\"especialidadMedica\":\"4\"}"
  echo "DAU $ID cargado"
}

crear_dau "310001" 126100 "03062026" "08:00" "08:05" "08:20" "09:10" "01" "02" "DOLOR TORACICO" "DOLOR TORACICO NO ESPECIFICADO" "R07.4" "01"
crear_dau "310002" 126100 "03062026" "09:15" "09:25" "09:55" "11:30" "02" "03" "CEFALEA" "CEFALEA" "R51" "01"
crear_dau "310003" 121105 "04062026" "10:00" "10:12" "10:50" "12:10" "01" "04" "DOLOR ABDOMINAL" "DOLOR ABDOMINAL" "R10.4" "01"
crear_dau "310004" 121105 "04062026" "11:20" "11:28" "11:45" "13:40" "02" "02" "DIFICULTAD RESPIRATORIA" "DISNEA" "R06.0" "03"
crear_dau "310005" 121110 "05062026" "12:00" "12:20" "13:10" "15:30" "01" "05" "LESION MANO" "TRAUMATISMO SUPERFICIAL" "S60.9" "01"
crear_dau "310006" 121110 "05062026" "14:30" "14:35" "14:50" "15:20" "02" "01" "DOLOR PRECORDIAL" "DOLOR PRECORDIAL" "R07.2" "03"
crear_dau "310007" 126100 "06062026" "15:10" "15:18" "15:40" "17:00" "01" "03" "FIEBRE" "FIEBRE NO ESPECIFICADA" "R50.9" "01"
crear_dau "310008" 126100 "06062026" "16:45" "17:00" "17:50" "20:10" "02" "04" "VOMITOS" "NAUSEAS Y VOMITOS" "R11" "01"
crear_dau "310009" 121120 "07062026" "18:00" "18:07" "18:30" "19:20" "01" "02" "CRISIS DE PANICO" "ANSIEDAD" "F41.9" "04"
crear_dau "310010" 121120 "07062026" "21:15" "21:30" "22:10" "23:50" "02" "03" "INTOXICACION" "EFECTO TOXICO" "T65.9" "03"
crear_dau "310011" 126100 "08062026" "00:20" "00:28" "01:00" "02:40" "01" "03" "TRAUMA" "TRAUMATISMO" "T14.9" "01"
crear_dau "310012" 121105 "08062026" "03:30" "03:45" "04:30" "06:20" "02" "04" "DOLOR LUMBAR" "LUMBAGO" "M54.5" "01"
crear_dau "310013" 121110 "09062026" "07:50" "07:56" "08:15" "09:00" "01" "02" "ASMA" "ASMA NO ESPECIFICADA" "J45.9" "01"
crear_dau "310014" 121120 "09062026" "13:00" "13:25" "14:20" "17:45" "02" "05" "CONTROL HERIDA" "HERIDA" "T14.1" "02"
crear_dau "310015" 126100 "10062026" "19:10" "19:16" "19:40" "21:00" "01" "02" "DOLOR TORACICO" "DOLOR TORACICO NO ESPECIFICADO" "R07.4" "03"

echo "Carga finalizada. Refresca Gestión Red."
