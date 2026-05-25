import { ReactKeycloakProvider } from "@react-keycloak/web";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import keycloak from "./services/keycloak.js";
import "./index.css";

// Opciones minimas para evitar el iframe 3p-cookies de Keycloak.
// Ese iframe usa document.requestStorageAccess() que esta bloqueado en
// HTTP (no-secure context) en Firefox/Zen. Si lo dejamos activo,
// keycloak.init() queda colgado para siempre en "Cargando...".
createRoot(document.getElementById("root")).render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{
      onLoad: "login-required",
      pkceMethod: "S256",
      checkLoginIframe: false,
    }}
  >
    <App />
  </ReactKeycloakProvider>,
);
