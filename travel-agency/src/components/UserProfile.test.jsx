import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import UserProfile from "./UserProfile.jsx";

const serviceMocks = vi.hoisted(() => ({
  getMe: vi.fn(),
  updateProfile: vi.fn(),
}));

vi.mock("../services/user.service", () => ({
  default: {
    getMe: serviceMocks.getMe,
    updateProfile: serviceMocks.updateProfile,
  },
}));

describe("UserProfile", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("loads the current profile and saves edited contact data", async () => {
    serviceMocks.getMe.mockResolvedValueOnce({
      data: {
        id: 1,
        fullName: "Matias Perez",
        email: "matias@example.com",
        phone: "+56 9 1111 2222",
        identityDocumentType: "RUT",
        identityDocumentNumber: "11.111.111-1",
        nationality: "Chilena",
        createdAt: "2026-04-22T10:30:00",
      },
    });

    serviceMocks.updateProfile.mockResolvedValueOnce({
      data: {
        id: 1,
        fullName: "Matias Perez",
        email: "matias@example.com",
        phone: "+56 9 9999 8888",
        identityDocumentType: "RUT",
        identityDocumentNumber: "11.111.111-1",
        nationality: "Argentina",
        createdAt: "2026-04-22T10:30:00",
      },
    });

    render(<UserProfile />);

    expect(screen.getByText("Cargando tu perfil...")).toBeInTheDocument();

    expect(await screen.findByDisplayValue("Matias Perez")).toBeInTheDocument();
    expect(screen.getByDisplayValue("matias@example.com")).toBeInTheDocument();

    const phoneInput = screen.getByLabelText("Teléfono");
    const nationalityInput = screen.getByLabelText("Nacionalidad");

    await userEvent.clear(phoneInput);
    await userEvent.type(phoneInput, "+56 9 9999 8888");
    await userEvent.clear(nationalityInput);
    await userEvent.type(nationalityInput, "Argentina");

    await userEvent.click(screen.getByRole("button", { name: "Guardar perfil" }));

    await waitFor(() => {
      expect(serviceMocks.updateProfile).toHaveBeenCalledWith({
        phone: "+56 9 9999 8888",
        identityDocumentType: "RUT",
        identityDocumentNumber: "11.111.111-1",
        nationality: "Argentina",
      });
    });

    expect(await screen.findByText("Perfil actualizado correctamente.")).toBeInTheDocument();
  });
});
