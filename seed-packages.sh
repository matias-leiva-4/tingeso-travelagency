#!/bin/bash
# Script para poblar la BD con 10 paquetes turisticos de ejemplo.
# Usa el endpoint POST /api/packages con un token de admin obtenido de Keycloak.
#
# Pre-requisitos:
#   - Stack levantado: docker compose up -d
#   - Usuario admin1 / admin123 con rol ADMIN en el realm travel-agency
#   - Direct access grants habilitado en el client travel-agency-frontend1
#
# Uso:
#   chmod +x seed-packages.sh
#   ./seed-packages.sh

set -e

KEYCLOAK_URL="http://keycloak:9090"
API_URL="http://localhost:8090"
REALM="travel-agency"
CLIENT_ID="travel-agency-frontend1"
ADMIN_USER="admin@admin.cl"
ADMIN_PASS="admin123"

echo "Obteniendo token de admin..."
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=${CLIENT_ID}" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASS}" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "ERROR: no se pudo obtener el token de admin."
  echo "Revisa que:"
  echo "  - Keycloak este corriendo en ${KEYCLOAK_URL}"
  echo "  - El usuario ${ADMIN_USER} exista con password ${ADMIN_PASS}"
  echo "  - 'Direct access grants' este activado en el client"
  exit 1
fi

echo "Token obtenido. Creando paquetes..."

create_package() {
  local body="$1"
  local name=$(echo "$body" | grep -o '"name":"[^"]*' | cut -d'"' -f4)

  HTTP_CODE=$(curl -s -o /tmp/seed-response.json -w "%{http_code}" \
    -X POST "${API_URL}/api/packages" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$body")

  if [ "$HTTP_CODE" = "201" ]; then
    echo "  ✓ Creado: ${name}"
  else
    echo "  ✗ FALLO (${HTTP_CODE}): ${name}"
    cat /tmp/seed-response.json
    echo ""
  fi
}

# Paquete 1
create_package '{
  "name": "Tour Patagonia",
  "destination": "Patagonia, Chile",
  "description": "Aventura en el sur del mundo: Torres del Paine, glaciares y caminatas en la cordillera.",
  "startDate": "2026-12-01",
  "endDate": "2026-12-10",
  "price": 1500000,
  "packageType": "Aventura",
  "includedServices": "Vuelos, hotel 4 estrellas, traslados, guia, 3 trekkings",
  "restrictions": "Edad minima 14 anios. Buena condicion fisica requerida.",
  "totalSlots": 20
}'

# Paquete 2
create_package '{
  "name": "Viaje Miami",
  "destination": "Miami, Florida, EEUU",
  "description": "Sol, playa y vida nocturna en South Beach. Incluye city tour y dia de compras.",
  "startDate": "2027-02-04",
  "endDate": "2027-02-14",
  "price": 250000,
  "packageType": "Familiar",
  "includedServices": "Vuelos, hotel frente al mar, desayunos, city tour, traslados aeropuerto",
  "restrictions": "Visa USA vigente. Pasaporte con 6 meses de validez.",
  "totalSlots": 60
}'

# Paquete 3
create_package '{
  "name": "Universal Studios Orlando",
  "destination": "Orlando, Florida, EEUU",
  "description": "Parques tematicos para toda la familia: Harry Potter, Jurassic Park, Minions.",
  "startDate": "2026-07-15",
  "endDate": "2026-07-25",
  "price": 50000,
  "packageType": "Familiar",
  "includedServices": "Hotel, entradas a 3 parques, traslados, desayunos",
  "restrictions": "Visa USA. Apto para ninios desde 5 anios.",
  "totalSlots": 24
}'

# Paquete 4
create_package '{
  "name": "Tour Tokio Tradicional",
  "destination": "Tokio, Japon",
  "description": "Templos, jardines zen, gastronomia y la energia de Shibuya. Visita al monte Fuji incluida.",
  "startDate": "2026-10-05",
  "endDate": "2026-10-15",
  "price": 800000,
  "packageType": "Cultural",
  "includedServices": "Vuelos, hotel en Shinjuku, JR Pass 7 dias, guia en espaniol, tour al Fuji",
  "restrictions": "Requiere pasaporte vigente. Apto mayores de 12 anios.",
  "totalSlots": 15
}'

# Paquete 5
create_package '{
  "name": "Paris Romantico",
  "destination": "Paris, Francia",
  "description": "La ciudad del amor: Torre Eiffel, Louvre, paseo en bateau-mouche por el Sena.",
  "startDate": "2026-09-10",
  "endDate": "2026-09-18",
  "price": 700000,
  "packageType": "Romantico",
  "includedServices": "Vuelos, hotel boutique, desayunos, cena Eiffel, entrada al Louvre",
  "restrictions": "Solo parejas o adultos. Schengen vigente.",
  "totalSlots": 12
}'

# Paquete 6
create_package '{
  "name": "Cusco Magico y Machu Picchu",
  "destination": "Cusco, Peru",
  "description": "Recorrido por el Valle Sagrado de los Incas y la ciudadela perdida de Machu Picchu.",
  "startDate": "2026-08-20",
  "endDate": "2026-08-27",
  "price": 300000,
  "packageType": "Cultural",
  "includedServices": "Vuelos, hotel en Cusco, tren a Machu Picchu, guia arqueologo, todas las entradas",
  "restrictions": "Aclimatacion recomendada para altura. Apto desde 10 anios.",
  "totalSlots": 25
}'

# Paquete 7
create_package '{
  "name": "Relax Tropical en Bali",
  "destination": "Bali, Indonesia",
  "description": "Spa, yoga al amanecer, arrozales de Tegallalang y templos sagrados. Villas privadas.",
  "startDate": "2026-11-12",
  "endDate": "2026-11-22",
  "price": 400000,
  "packageType": "Relajo",
  "includedServices": "Vuelos, villa con piscina privada, 5 sesiones de spa, clases de yoga",
  "restrictions": "Minimo 18 anios. No incluye seguro medico.",
  "totalSlots": 18
}'

# Paquete 8
create_package '{
  "name": "New York Express",
  "destination": "Nueva York, EEUU",
  "description": "La Gran Manzana en 5 dias: Times Square, Estatua de la Libertad, Central Park, Broadway.",
  "startDate": "2026-12-15",
  "endDate": "2026-12-21",
  "price": 650000,
  "packageType": "Cultural",
  "includedServices": "Vuelos, hotel en Manhattan, City Pass, entrada a un musical de Broadway",
  "restrictions": "Visa USA. Pasaporte vigente.",
  "totalSlots": 30
}'

# Paquete 9
create_package '{
  "name": "Roma Antigua",
  "destination": "Roma, Italia",
  "description": "Coliseo, Vaticano, Foro Romano y la mejor pasta. Tour guiado por arqueologos.",
  "startDate": "2026-09-22",
  "endDate": "2026-09-30",
  "price": 550000,
  "packageType": "Cultural",
  "includedServices": "Vuelos, hotel cerca del Vaticano, desayunos, tours guiados",
  "restrictions": "Pasaporte Schengen vigente. Buena disposicion para caminar.",
  "totalSlots": 20
}'

# Paquete 10
create_package '{
  "name": "Desierto de Atacama Estrellas",
  "destination": "San Pedro de Atacama, Chile",
  "description": "El cielo mas despejado del mundo: observatorio astronomico, geyseres del Tatio, laguna Cejar.",
  "startDate": "2026-07-01",
  "endDate": "2026-07-06",
  "price": 350000,
  "packageType": "Aventura",
  "includedServices": "Vuelos a Calama, traslados, hotel boutique, tour astronomico, desayunos",
  "restrictions": "Edad minima 8 anios. Recomendado aclimatarse 1 dia por altura.",
  "totalSlots": 16
}'

echo ""
echo "Listo. Verificando..."
curl -s "${API_URL}/api/packages" | grep -o '"name":"[^"]*' | cut -d'"' -f4 | nl
