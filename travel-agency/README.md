# TravelAgency Frontend

Frontend built with React + Vite + Material UI.

## Development

```bash
npm install
npm run dev
```

## Production Build

```bash
npm run build
```

## Run With Docker + Nginx

Build the image:

```bash
docker build \
  --build-arg VITE_API_BASE_URL=http://localhost:8080 \
  --build-arg VITE_KEYCLOAK_URL=http://localhost:9090 \
  --build-arg VITE_KEYCLOAK_REALM=travel-agency \
  --build-arg VITE_KEYCLOAK_CLIENT_ID=travel-agency-frontend \
  -t travel-agency-frontend .
```

Run the container:

```bash
docker run --rm -p 5173:80 travel-agency-frontend
```

Then open:

```txt
http://localhost:5173
```
