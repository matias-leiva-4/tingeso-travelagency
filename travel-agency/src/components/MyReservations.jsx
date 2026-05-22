import PaymentsIcon from "@mui/icons-material/Payments";
import {
  Button,
  Container,
  Grid,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useCallback, useEffect, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import reservationService from "../services/reservation.service";
import { formatCurrency, formatDateTime } from "../utils/formatters.js";
import EmptyState from "./EmptyState.jsx";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import StatusChip from "./StatusChip.jsx";

function MyReservations() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submittingId, setSubmittingId] = useState(null);
  const [error, setError] = useState("");

  const loadReservations = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await reservationService.getByUser();
      setReservations(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo cargar las reservas.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReservations();
  }, [loadReservations]);

  const handleCancel = async (reservationId) => {
    setSubmittingId(reservationId);
    setError("");
    try {
      const response = await reservationService.cancel(reservationId);
      setReservations((current) =>
        current.map((r) => (r.id === reservationId ? response.data : r)),
      );
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo cancelar la reserva.",
      );
    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom fontWeight={700}>
        Mis reservas
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Revisa el estado de tus reservas, paga las pendientes o cancela antes de confirmar.
      </Typography>

      <Stack spacing={3}>
        <ErrorAlert message={error} />

        {loading ? (
          <LoadingState message="Cargando tus reservas..." />
        ) : reservations.length === 0 ? (
          <EmptyState
            title="Aún no tienes reservas"
            description="Explora el catálogo y crea tu primera reserva."
          />
        ) : (
          <Grid container spacing={3}>
            {reservations.map((reservation) => {
              const canAct = reservation.status === "PENDING_PAYMENT";

              return (
                <Grid key={reservation.id} size={{ xs: 12, md: 6 }}>
                  <Paper sx={{ p: 3 }}>
                    <Stack spacing={2}>
                      <Stack
                        direction="row"
                        justifyContent="space-between"
                        spacing={2}
                      >
                        <Stack spacing={0.5}>
                          <Typography variant="overline" color="primary.dark">
                            Reserva #{reservation.id}
                          </Typography>
                          <Typography variant="h5" component="h2">
                            {reservation.packageName}
                          </Typography>
                          <Typography color="text.secondary">
                            {reservation.packageDestination}
                          </Typography>
                        </Stack>
                        <StatusChip status={reservation.status} />
                      </Stack>

                      <Typography>
                        {reservation.passengerCount} pasajeros · Total{" "}
                        {formatCurrency(reservation.finalAmount)}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Creada el {formatDateTime(reservation.createdAt)}
                      </Typography>

                      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
                        <Button
                          component={RouterLink}
                          to={`/reservations/${reservation.id}`}
                          variant="outlined"
                        >
                          Ver detalle
                        </Button>
                        <Button
                          component={RouterLink}
                          to={`/payments/${reservation.id}`}
                          variant="contained"
                          startIcon={<PaymentsIcon />}
                          disabled={!canAct}
                        >
                          Pagar
                        </Button>
                        <Button
                          color="error"
                          variant="text"
                          disabled={!canAct || submittingId === reservation.id}
                          onClick={() => handleCancel(reservation.id)}
                        >
                          Cancelar
                        </Button>
                      </Stack>
                    </Stack>
                  </Paper>
                </Grid>
              );
            })}
          </Grid>
        )}
      </Stack>
    </Container>
  );
}

export default MyReservations;
