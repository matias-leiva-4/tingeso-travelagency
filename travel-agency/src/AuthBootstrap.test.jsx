import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AuthBootstrap from "./AuthBootstrap.jsx";

vi.mock("@react-keycloak/web", () => ({
  ReactKeycloakProvider: ({ children }) => children,
}));

vi.mock("./App.jsx", () => ({
  default: () => <div>Cargando...</div>,
}));

vi.mock("./services/keycloak.js", () => ({
  default: {},
}));

describe("AuthBootstrap", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("muestra ayuda si Keycloak no termina de inicializar", () => {
    render(<AuthBootstrap />);

    expect(screen.getByText("Cargando...")).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(12000);
    });

    expect(
      screen.getByText("La autenticación no pudo iniciarse"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Keycloak también debe permitir redirecciones/),
    ).toBeInTheDocument();
  });
});
