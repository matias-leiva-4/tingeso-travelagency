import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import PersonIcon from "@mui/icons-material/Person";
import {
  Alert,
  AlertTitle,
  Button,
  Container,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";
import packageService from "../services/package.service";
import reservationService from "../services/reservation.service";
import userService from "../services/user.service";
import { formatCurrency, formatDate } from "../utils/formatters.js";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import ReservationSummary from "./ReservationSummary.jsx";

// Devuelve la lista de campos del perfil que aún faltan. Vacía → perfil completo.
function getMissingProfileFields(profile) {
  if (!profile) return ["perfil"];
  const missing = [];
  if (!profile.phone?.trim()) missing.push("teléfono");
  if (!profile.identityDocumentType) missing.push("tipo de documento");
  if (!profile.identityDocumentNumber?.trim()) missing.push("número de documento");
  return missing;
}

function ReservationCheckout() {
  const { packageId } = useParams();
  const navigate = useNavigate();
  const [packageItem, setPackageItem] = useState(null);
  const [profile, setProfile] = useState(null);
  const [passengerCount, setPassengerCount] = useState(1);
  const [reservation, setReservation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    // Cargamos paquete y perfil en paralelo: el perfil bloquea el checkout
    // si está incompleto (regla espejo de la del backend).
    const loadData = async () => {
      setLoading(true);
      setError("");
      try {
        const [pkgRes, profileRes] = await Promise.all([
          packageService.getById(packageId),
          userService.getMe(),
        ]);
        setPackageItem(pkgRes.data);
        setProfile(profileRes.data);
      } catch (err) {
        setError(
          err?.response?.data?.message ?? err?.message ?? "No se pudo cargar la información.",
        );
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [packageId]);

  const missingFields = getMissingProfileFields(profile);
  const profileIncomplete = !reservation && missingFields.length > 0;

  const handleCreateReservation = async (event) => {
    event.preventDefault();

    if (Number(passengerCount) < 1) {
      setError("La cantidad de pasajeros debe ser mayor a cero.");
      return;
    }

    const stillMissing = getMissingProfileFields(profile);
    if (stillMissing.length > 0) {
      setError(`Completa tu perfil antes de reservar. Faltan: ${stillMissing.join(", ")}.`);
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const response = await reservationService.create({
        packageId: Number(packageId),
        passengerCount: Number(passengerCount),
      });
      setReservation(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo crear la reserva.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async () => {
    if (!reservation) return;

    setSubmitting(true);
    setError("");

    try {
      const response = await reservationService.cancel(reservation.id);
      setReservation(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo cancelar la reserva.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <LoadingState message="Preparando reserva..." />;
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Button
          component={RouterLink}
          to={`/packages/${packageId}`}
          startIcon={<ArrowBackIcon />}
          sx={{ alignSelf: "flex-start" }}
        >
          Volver al detalle
        </Button>

        <ErrorAlert message={error} />

        {profileIncomplete && (
          <Alert
            severity="warning"
            icon={<PersonIcon />}
            action={
              <Button
                component={RouterLink}
                to="/profile"
                color="inherit"
                size="small"
                variant="outlined"
              >
                Completar perfil
              </Button>
            }
          >
            <AlertTitle>Perfil incompleto</AlertTitle>
            No puedes reservar hasta completar: <strong>{missingFields.join(", ")}</strong>.
            El documento de identidad es necesario para emitir tu reserva de viaje.
          </Alert>
        )}

        {reservation ? (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 7 }}>
              <ReservationSummary reservation={reservation} />
            </Grid>
            <Grid size={{ xs: 12, md: 5 }}>
              <Paper sx={{ p: 3 }}>
                <Stack spacing={2}>
                  <Alert severity="success">
                    Reserva creada. Revisa el desglose antes de pagar.
                  </Alert>
                  <Button
                    variant="contained"
                    size="large"
                    onClick={() => navigate(`/payments/${reservation.id}`)}
                    disabled={reservation.status !== "PENDING_PAYMENT"}
                  >
                    Pagar ahora
                  </Button>
                  <Button
                    color="error"
                    variant="outlined"
                    onClick={handleCancel}
                    disabled={submitting || reservation.status !== "PENDING_PAYMENT"}
                  >
                    Cancelar reserva
                  </Button>
                </Stack>
              </Paper>
            </Grid>
          </Grid>
        ) : (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 7 }}>
              <Paper sx={{ p: 3 }}>
                <Stack spacing={2}>
                  <Typography variant="overline" color="primary.dark">
                    {packageItem?.destination}
                  </Typography>
                  <Typography variant="h4" component="h1">
                    Confirmar reserva
                  </Typography>
                  <Typography color="text.secondary">{packageItem?.name}</Typography>
                  <Typography>
                    Viaje desde {formatDate(packageItem?.startDate)} hasta{" "}
                    {formatDate(packageItem?.endDate)}
                  </Typography>
                  <Typography variant="h5" color="primary.dark">
                    {formatCurrency(packageItem?.price)} por pasajero
                  </Typography>
                </Stack>
              </Paper>
            </Grid>
            <Grid size={{ xs: 12, md: 5 }}>
              <Paper component="form" onSubmit={handleCreateReservation} sx={{ p: 3 }}>
                <Stack spacing={2}>
                  <Typography variant="h5" component="h2">
                    Datos de reserva
                  </Typography>
                  <TextField
                    label="Cantidad de pasajeros"
                    type="number"
                    value={passengerCount}
                    onChange={(event) => setPassengerCount(event.target.value)}
                    slotProps={{
                      htmlInput: { min: 1, max: packageItem?.availableSlots ?? 1 },
                    }}
                    helperText={`${packageItem?.availableSlots ?? 0} cupos disponibles`}
                    fullWidth
                  />
                  <Alert severity="info">
                    El backend calculará los descuentos oficiales al crear la reserva.
                  </Alert>
                  <Button
                    type="submit"
                    variant="contained"
                    size="large"
                    disabled={submitting || profileIncomplete}
                  >
                    Crear reserva
                  </Button>
                </Stack>
              </Paper>
            </Grid>
          </Grid>
        )}
      </Stack>
    </Container>
  );
}

export default ReservationCheckout;
