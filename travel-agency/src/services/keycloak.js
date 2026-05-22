import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:9090",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "travel-agency",
  clientId:
    import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "travel-agency-frontend1",
});

export default keycloak;
