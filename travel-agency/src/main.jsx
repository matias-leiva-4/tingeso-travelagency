import { ReactKeycloakProvider } from "@react-keycloak/web";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import keycloak from "./services/keycloak.js";
import "./index.css";

createRoot(document.getElementById("root")).render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{ pkceMethod: "S256", checkLoginIframe: false }}
  >
    <App />
  </ReactKeycloakProvider>,
);
