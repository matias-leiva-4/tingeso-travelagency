import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import SaveIcon from "@mui/icons-material/Save";
import {
  Alert,
  Button,
  Container,
  Grid,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import adminService from "../services/admin.service";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";

const packageTypes = ["Aventura", "Cultural", "Familiar", "Romántico", "Relajo"];

const initialForm = {
  name: "",
  destination: "",
  description: "",
  startDate: "",
  endDate: "",
  price: "",
  packageType: "",
  includedServices: "",
  restrictions: "",
  totalSlots: "",
};

function validateForm(form) {
  const errors = {};

  if (!form.name.trim()) {
    errors.name = "El nombre es obligatorio.";
  }
  if (!form.destination.trim()) {
    errors.destination = "El destino es obligatorio.";
  }
  if (!form.description.trim()) {
    errors.description = "La descripción es obligatoria.";
  }
  if (!form.startDate) {
    errors.startDate = "La fecha de inicio es obligatoria.";
  }
  if (!form.endDate) {
    errors.endDate = "La fecha de término es obligatoria.";
  }
  if (form.startDate && form.endDate && form.startDate > form.endDate) {
    errors.endDate = "La fecha de término debe ser posterior a la de inicio.";
  }
  if (!form.price || Number(form.price) <= 0) {
    errors.price = "El precio debe ser mayor a 0.";
  }
  if (!form.packageType) {
    errors.packageType = "Selecciona un tipo de experiencia.";
  }
  if (!form.totalSlots || Number(form.totalSlots) <= 0) {
    errors.totalSlots = "Los cupos deben ser mayor a 0.";
  }

  return errors;
}

function PackageForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditing = Boolean(id);

  const [form, setForm] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  // Cuántos cupos del paquete ya están reservados. Si > 0, no se pueden
  // modificar fechas ni reducir totalSlots bajo este número (regla Épica 2).
  const [reservedSlots, setReservedSlots] = useState(0);

  useEffect(() => {
    if (!isEditing) return;

    const loadPackage = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await adminService.getAllPackages();
        const pkg = response.data.find((p) => p.id === Number(id));
        if (!pkg) {
          setError("Paquete no encontrado.");
          return;
        }
        setForm({
          name: pkg.name ?? "",
          destination: pkg.destination ?? "",
          description: pkg.description ?? "",
          startDate: pkg.startDate ?? "",
          endDate: pkg.endDate ?? "",
          price: pkg.price ?? "",
          packageType: pkg.packageType ?? "",
          includedServices: pkg.includedServices ?? "",
          restrictions: pkg.restrictions ?? "",
          totalSlots: pkg.totalSlots ?? "",
        });
        setReservedSlots((pkg.totalSlots ?? 0) - (pkg.availableSlots ?? 0));
      } catch (err) {
        setError(
          err?.response?.data?.message ??
            err?.message ??
            "No se pudo cargar el paquete.",
        );
      } finally {
        setLoading(false);
      }
    };

    loadPackage();
  }, [id, isEditing]);

  const updateField = (field) => (event) => {
    setForm((current) => ({
      ...current,
      [field]: event.target.value,
    }));
    if (formErrors[field]) {
      setFormErrors((current) => {
        const updated = { ...current };
        delete updated[field];
        return updated;
      });
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const errors = validateForm(form);
    if (isEditing && reservedSlots > 0 && Number(form.totalSlots) < reservedSlots) {
      errors.totalSlots = `No puedes reducir los cupos por debajo de ${reservedSlots} (ya reservados).`;
    }
    setFormErrors(errors);

    if (Object.keys(errors).length > 0) {
      return;
    }

    setSubmitting(true);
    setError("");
    setSuccess("");

    const payload = {
      name: form.name.trim(),
      destination: form.destination.trim(),
      description: form.description.trim(),
      startDate: form.startDate,
      endDate: form.endDate,
      price: Number(form.price),
      packageType: form.packageType,
      includedServices: form.includedServices.trim(),
      restrictions: form.restrictions.trim(),
      totalSlots: Number(form.totalSlots),
    };

    try {
      if (isEditing) {
        await adminService.updatePackage(id, payload);
        setSuccess("Paquete actualizado correctamente.");
      } else {
        await adminService.createPackage(payload);
        setSuccess("Paquete creado correctamente.");
      }
      setTimeout(() => navigate("/admin/packages"), 1200);
    } catch (err) {
      setError(
        err?.response?.data?.message ??
          err?.message ??
          "No se pudo guardar el paquete.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <LoadingState message="Cargando paquete..." />;
  }

  const hasReservations = isEditing && reservedSlots > 0;

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate("/admin/packages")}
          sx={{ alignSelf: "flex-start" }}
        >
          Volver a paquetes
        </Button>

        <Typography variant="h4" component="h1" fontWeight={700}>
          {isEditing ? "Editar Paquete" : "Nuevo Paquete"}
        </Typography>

        <ErrorAlert message={error} />
        {success && <Alert severity="success">{success}</Alert>}
        {hasReservations && (
          <Alert severity="warning">
            Este paquete tiene <strong>{reservedSlots} cupo(s) reservado(s)</strong>.
            Por regla de negocio no puedes modificar las fechas ni reducir los cupos
            totales por debajo de los ya reservados.
          </Alert>
        )}

        <Paper component="form" onSubmit={handleSubmit} sx={{ p: 3 }}>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                label="Nombre"
                value={form.name}
                onChange={updateField("name")}
                error={Boolean(formErrors.name)}
                helperText={formErrors.name}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                label="Destino"
                value={form.destination}
                onChange={updateField("destination")}
                error={Boolean(formErrors.destination)}
                helperText={formErrors.destination}
              />
            </Grid>
            <Grid size={{ xs: 12 }}>
              <TextField
                fullWidth
                multiline
                minRows={3}
                label="Descripción"
                value={form.description}
                onChange={updateField("description")}
                error={Boolean(formErrors.description)}
                helperText={formErrors.description}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                fullWidth
                type="date"
                label="Fecha de inicio"
                value={form.startDate}
                onChange={updateField("startDate")}
                disabled={hasReservations}
                error={Boolean(formErrors.startDate)}
                helperText={
                  formErrors.startDate ??
                  (hasReservations ? "Bloqueada por reservas activas" : "")
                }
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                fullWidth
                type="date"
                label="Fecha de término"
                value={form.endDate}
                onChange={updateField("endDate")}
                disabled={hasReservations}
                error={Boolean(formErrors.endDate)}
                helperText={
                  formErrors.endDate ??
                  (hasReservations ? "Bloqueada por reservas activas" : "")
                }
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                fullWidth
                type="number"
                label="Precio"
                value={form.price}
                onChange={updateField("price")}
                error={Boolean(formErrors.price)}
                helperText={formErrors.price}
                slotProps={{ htmlInput: { min: 0 } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                fullWidth
                type="number"
                label="Cupos totales"
                value={form.totalSlots}
                onChange={updateField("totalSlots")}
                error={Boolean(formErrors.totalSlots)}
                helperText={
                  formErrors.totalSlots ??
                  (hasReservations
                    ? `Mínimo permitido: ${reservedSlots} (cupos reservados)`
                    : "")
                }
                slotProps={{ htmlInput: { min: Math.max(1, reservedSlots) } }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                select
                label="Tipo de experiencia"
                value={form.packageType}
                onChange={updateField("packageType")}
                error={Boolean(formErrors.packageType)}
                helperText={formErrors.packageType}
              >
                <MenuItem value="">Seleccionar...</MenuItem>
                {packageTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12 }}>
              <TextField
                fullWidth
                multiline
                minRows={2}
                label="Servicios incluidos"
                value={form.includedServices}
                onChange={updateField("includedServices")}
                helperText="Describe los servicios incluidos en el paquete."
              />
            </Grid>
            <Grid size={{ xs: 12 }}>
              <TextField
                fullWidth
                multiline
                minRows={2}
                label="Restricciones"
                value={form.restrictions}
                onChange={updateField("restrictions")}
                helperText="Indica restricciones o condiciones especiales."
              />
            </Grid>
            <Grid size={{ xs: 12 }}>
              <Stack direction="row" spacing={2} justifyContent="flex-end">
                <Button
                  variant="text"
                  onClick={() => navigate("/admin/packages")}
                  disabled={submitting}
                >
                  Cancelar
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<SaveIcon />}
                  disabled={submitting}
                >
                  {isEditing ? "Guardar cambios" : "Crear paquete"}
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </Paper>
      </Stack>
    </Container>
  );
}

export default PackageForm;
