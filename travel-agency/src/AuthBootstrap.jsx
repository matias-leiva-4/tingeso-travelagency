import { Alert, AlertTitle, Box, Button, Typography } from "@mui/material";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import { useEffect, useState } from "react";
import App from "./App.jsx";
import keycloak from "./services/keycloak.js";

const keycloakUrl =
  import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:9090";
const initializationTimeoutMs = 12000;
const isSecureContext = window.isSecureContext === true;

function installHttpRandomUuidFallback() {
  if (
    isSecureContext ||
    typeof globalThis.crypto?.randomUUID === "function" ||
    typeof globalThis.crypto?.getRandomValues !== "function"
  ) {
    return;
  }

  globalThis.crypto.randomUUID = () => {
    const bytes = globalThis.crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;

    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0"))
      .join("")
      .replace(
        /^(.{8})(.{4})(.{4})(.{4})(.{12})$/,
        "$1-$2-$3-$4-$5",
      );
  };
}

installHttpRandomUuidFallback();

function AuthBootstrap() {
  const [authenticationState, setAuthenticationState] = useState("starting");

  useEffect(() => {
    if (authenticationState !== "starting") {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      setAuthenticationState("timeout");
    }, initializationTimeoutMs);

    return () => window.clearTimeout(timer);
  }, [authenticationState]);

  const handleKeycloakEvent = (eventType, error) => {
    if (eventType === "onInitError") {
      console.error("No fue posible inicializar Keycloak.", error);
      setAuthenticationState("error");
    } else if (eventType === "onReady") {
      setAuthenticationState("ready");
    }
  };

  if (authenticationState === "error" || authenticationState === "timeout") {
    return (
      <Box sx={{ maxWidth: 760, mx: "auto", px: 3, py: 8 }}>
        <Alert severity="error">
          <AlertTitle>La autenticación no pudo iniciarse</AlertTitle>
          <Typography sx={{ mb: 1 }}>
            El frontend no logró abrir el inicio de sesión de Keycloak. La URL
            configurada debe ser pública y accesible desde este navegador:
          </Typography>
          <Typography
            component="code"
            sx={{ display: "block", mb: 1, overflowWrap: "anywhere" }}
          >
            {keycloakUrl}
          </Typography>
          <Typography sx={{ mb: 2 }}>
            Keycloak también debe permitir redirecciones desde{" "}
            <code>{window.location.origin}</code>.
          </Typography>
          <Button variant="outlined" onClick={() => window.location.reload()}>
            Reintentar
          </Button>
        </Alert>
      </Box>
    );
  }

  // Sobre HTTP remoto el navegador no expone SubtleCrypto, requerido por PKCE.
  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{
        onLoad: "login-required",
        pkceMethod: isSecureContext ? "S256" : false,
        checkLoginIframe: false,
      }}
      onEvent={handleKeycloakEvent}
    >
      <App />
    </ReactKeycloakProvider>
  );
}

export default AuthBootstrap;
