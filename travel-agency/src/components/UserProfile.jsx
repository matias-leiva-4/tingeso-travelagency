import PersonIcon from "@mui/icons-material/Person";
import SaveIcon from "@mui/icons-material/Save";
import {
  Alert,
  Button,
  Chip,
  Container,
  Grid,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useCallback, useEffect, useMemo, useState } from "react";
import userService from "../services/user.service";
import { formatDateTime, formatRut } from "../utils/formatters.js";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import { PageHeader } from "./PageHeader.jsx";

const DOCUMENT_TYPE_OPTIONS = [
  { value: "", label: "Selecciona un tipo" },
  { value: "RUT", label: "RUT" },
  { value: "PASSPORT", label: "Pasaporte" },
];

const EMPTY_FORM = {
  phone: "",
  identityDocumentType: "",
  identityDocumentNumber: "",
  nationality: "",
};

function UserProfile() {
  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const loadProfile = useCallback(async () => {
    setLoading(true);
    setError("");
    setSuccessMessage("");

    try {
      const response = await userService.getMe();
      const userProfile = response.data;
      setProfile(userProfile);
      setFormData({
        phone: userProfile.phone ?? "",
        identityDocumentType: userProfile.identityDocumentType ?? "",
        identityDocumentNumber:
          userProfile.identityDocumentType === "RUT"
            ? formatRut(userProfile.identityDocumentNumber)
            : (userProfile.identityDocumentNumber ?? ""),
        nationality: userProfile.nationality ?? "",
      });
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo cargar tu perfil.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const missingFields = useMemo(() => {
    return [
      !formData.phone && "teléfono",
      !formData.identityDocumentType && "tipo de documento",
      !formData.identityDocumentNumber && "número de documento",
      !formData.nationality && "nacionalidad",
    ].filter(Boolean);
  }, [formData]);

  const isProfileComplete = missingFields.length === 0;

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((current) => {
      if (name === "identityDocumentType") {
        return {
          ...current,
          identityDocumentType: value,
          identityDocumentNumber:
            value === "RUT"
              ? formatRut(current.identityDocumentNumber)
              : current.identityDocumentNumber,
        };
      }

      if (name === "identityDocumentNumber") {
        return {
          ...current,
          identityDocumentNumber:
            current.identityDocumentType === "RUT" ? formatRut(value) : value,
        };
      }

      return {
        ...current,
        [name]: value,
      };
    });

    setSuccessMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccessMessage("");

    try {
      const payload = {
        phone: formData.phone.trim(),
        identityDocumentType: formData.identityDocumentType || null,
        identityDocumentNumber: formData.identityDocumentNumber.trim(),
        nationality: formData.nationality.trim(),
      };

      const response = await userService.updateProfile(payload);
      const updatedProfile = response.data;

      setProfile(updatedProfile);
      setFormData({
        phone: updatedProfile.phone ?? "",
        identityDocumentType: updatedProfile.identityDocumentType ?? "",
        identityDocumentNumber:
          updatedProfile.identityDocumentType === "RUT"
            ? formatRut(updatedProfile.identityDocumentNumber)
            : (updatedProfile.identityDocumentNumber ?? ""),
        nationality: updatedProfile.nationality ?? "",
      });
      setSuccessMessage("Perfil actualizado correctamente.");
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo actualizar tu perfil.",
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <LoadingState message="Cargando tu perfil..." />;
  }

  return (
    <>
      <PageHeader
        eyebrow="Cuenta del viajero"
        title="Mi perfil"
        description="Completa tus datos de contacto y documento para que tus próximas reservas sean más rápidas."
        action={
          <Chip
            icon={<PersonIcon />}
            color={isProfileComplete ? "success" : "warning"}
            label={isProfileComplete ? "Perfil completo" : "Perfil pendiente"}
            sx={{ color: "white", borderColor: "rgba(255,255,255,0.4)" }}
            variant="outlined"
          />
        }
      />

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 7 }}>
            <Paper component="form" onSubmit={handleSubmit} sx={{ p: 3 }}>
              <Stack spacing={3}>
                <Stack spacing={1}>
                  <Typography variant="h5" component="h2">
                    Datos editables
                  </Typography>
                  <Typography color="text.secondary">
                    Estos datos se guardan en la aplicación y se usan para futuras reservas.
                  </Typography>
                </Stack>

                <ErrorAlert message={error} />
                {successMessage ? <Alert severity="success">{successMessage}</Alert> : null}

                <TextField
                  label="Nombre completo"
                  value={profile?.fullName ?? ""}
                  helperText="Este dato lo administra Keycloak."
                  fullWidth
                  disabled
                />

                <TextField
                  label="Correo electrónico"
                  value={profile?.email ?? ""}
                  helperText="Si necesitas cambiarlo, debes hacerlo en tu cuenta de acceso."
                  fullWidth
                  disabled
                />

                <TextField
                  label="Teléfono"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="+56 9 1234 5678"
                  helperText="Máximo 20 caracteres."
                  fullWidth
                />

                <TextField
                  select
                  label="Tipo de documento"
                  name="identityDocumentType"
                  value={formData.identityDocumentType}
                  onChange={handleChange}
                  fullWidth
                >
                  {DOCUMENT_TYPE_OPTIONS.map((option) => (
                    <MenuItem key={option.value || "empty"} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </TextField>

                <TextField
                  label="Número de documento"
                  name="identityDocumentNumber"
                  value={formData.identityDocumentNumber}
                  onChange={handleChange}
                  placeholder="12.345.678-9 o número de pasaporte"
                  helperText={
                    formData.identityDocumentType === "RUT"
                      ? "Se formatea automáticamente como 12.345.678-9 o 1.234.567-8."
                      : "Máximo 30 caracteres."
                  }
                  fullWidth
                />

                <TextField
                  label="Nacionalidad"
                  name="nationality"
                  value={formData.nationality}
                  onChange={handleChange}
                  placeholder="Chilena"
                  helperText="Máximo 60 caracteres."
                  fullWidth
                />

                <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
                  <Button type="submit" variant="contained" startIcon={<SaveIcon />} disabled={saving}>
                    Guardar perfil
                  </Button>
                  <Button variant="outlined" onClick={loadProfile} disabled={saving}>
                    Recargar datos
                  </Button>
                </Stack>
              </Stack>
            </Paper>
          </Grid>

          <Grid size={{ xs: 12, md: 5 }}>
            <Stack spacing={3}>
              <Paper sx={{ p: 3 }}>
                <Stack spacing={2}>
                  <Typography variant="h6" component="h2">
                    Estado del perfil
                  </Typography>

                  {isProfileComplete ? (
                    <Alert severity="success">
                      Ya completaste los datos principales para reservar más rápido.
                    </Alert>
                  ) : (
                    <Alert severity="warning">
                      Aún faltan: {missingFields.join(", ")}. Te recomiendo completarlos antes de seguir reservando.
                    </Alert>
                  )}

                  <Typography variant="body2" color="text.secondary">
                    Usuario creado en la app el {formatDateTime(profile?.createdAt)}.
                  </Typography>
                </Stack>
              </Paper>

              <Paper sx={{ p: 3 }}>
                <Stack spacing={1.5}>
                  <Typography variant="h6" component="h2">
                    Qué se guarda aquí
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    El nombre y el correo vienen desde Keycloak.
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    El teléfono, el documento y la nacionalidad los completas dentro de TravelAgency.
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Esto ayuda a reutilizar tus datos cuando avances con reservas y pagos.
                  </Typography>
                </Stack>
              </Paper>
            </Stack>
          </Grid>
        </Grid>
      </Container>
    </>
  );
}

export default UserProfile;
