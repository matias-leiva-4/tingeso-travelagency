import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import ReceiptIcon from "@mui/icons-material/Receipt";
import {
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";
import reservationService from "../services/reservation.service";
import {
  formatCurrency,
  formatDate,
  formatDateTime,
} from "../utils/formatters.js";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import ReservationSummary from "./ReservationSummary.jsx";

function ReservationDetail() {
  const { reservationId } = useParams();
  const navigate = useNavigate();
  const [reservation, setReservation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  // Estado del comprobante: lo cargamos bajo demanda al abrir el diálogo.
  const [receipt, setReceipt] = useState(null);
  const [receiptOpen, setReceiptOpen] = useState(false);
  const [receiptLoading, setReceiptLoading] = useState(false);

  useEffect(() => {
    const loadReservation = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await reservationService.getById(reservationId);
        setReservation(response.data);
      } catch (err) {
        setError(
          err?.response?.data?.message ?? err?.message ?? "No se pudo cargar la reserva.",
        );
      } finally {
        setLoading(false);
      }
    };

    loadReservation();
  }, [reservationId]);

  const handleCancel = async () => {
    setSubmitting(true);
    setError("");
    try {
      const response = await reservationService.cancel(reservationId);
      setReservation(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo cancelar la reserva.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpenReceipt = async () => {
    setReceiptOpen(true);
    setReceiptLoading(true);
    setError("");
    try {
      const response = await reservationService.getReceipt(reservationId);
      setReceipt(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo cargar el comprobante.",
      );
      setReceiptOpen(false);
    } finally {
      setReceiptLoading(false);
    }
  };

  const handleCloseReceipt = () => {
    setReceiptOpen(false);
  };

  if (loading) {
    return <LoadingState message="Cargando reserva..." />;
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Button
          component={RouterLink}
          to="/my-reservations"
          startIcon={<ArrowBackIcon />}
          sx={{ alignSelf: "flex-start" }}
        >
          Volver a mis reservas
        </Button>

        <ErrorAlert message={error} />

        {reservation ? (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 7 }}>
              <ReservationSummary reservation={reservation} />
            </Grid>
            <Grid size={{ xs: 12, md: 5 }}>
              <Paper sx={{ p: 3 }}>
                <Stack spacing={2}>
                  <Typography variant="h5" component="h1">
                    Seguimiento
                  </Typography>
                  <Typography color="text.secondary">
                    Creada el {formatDateTime(reservation.createdAt)}
                  </Typography>
                  <Button
                    variant="contained"
                    onClick={() => navigate(`/payments/${reservation.id}`)}
                    disabled={reservation.status !== "PENDING_PAYMENT"}
                  >
                    Pagar reserva
                  </Button>
                  <Button
                    color="error"
                    variant="outlined"
                    onClick={handleCancel}
                    disabled={submitting || reservation.status !== "PENDING_PAYMENT"}
                  >
                    Cancelar reserva
                  </Button>
                  {reservation.status === "CONFIRMED" && (
                    <Button
                      variant="outlined"
                      color="success"
                      startIcon={<ReceiptIcon />}
                      onClick={handleOpenReceipt}
                    >
                      Ver comprobante
                    </Button>
                  )}
                </Stack>
              </Paper>
            </Grid>
          </Grid>
        ) : null}
      </Stack>

      <Dialog open={receiptOpen} onClose={handleCloseReceipt} maxWidth="sm" fullWidth>
        <DialogTitle>
          Comprobante de reserva {receipt ? `#${receipt.reservationId}` : ""}
        </DialogTitle>
        <DialogContent dividers>
          {receiptLoading || !receipt ? (
            <LoadingState message="Generando comprobante..." />
          ) : (
            <Stack spacing={2}>
              <Box>
                <Typography variant="overline" color="text.secondary">
                  Cliente
                </Typography>
                <Typography>{receipt.clientName}</Typography>
                <Typography color="text.secondary">{receipt.clientEmail}</Typography>
                {receipt.clientDocument && (
                  <Typography color="text.secondary">{receipt.clientDocument}</Typography>
                )}
              </Box>

              <Divider />

              <Box>
                <Typography variant="overline" color="text.secondary">
                  Paquete
                </Typography>
                <Typography variant="h6">{receipt.packageName}</Typography>
                <Typography color="text.secondary">{receipt.destination}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {formatDate(receipt.packageStartDate)} – {formatDate(receipt.packageEndDate)}
                </Typography>
              </Box>

              <Divider />

              <Box>
                <Typography variant="overline" color="text.secondary">
                  Detalle
                </Typography>
                <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%" }}>
                  <Typography>Pasajeros</Typography>
                  <Typography fontWeight={600}>{receipt.passengerCount}</Typography>
                </Box>
                <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%" }}>
                  <Typography>Precio base</Typography>
                  <Typography>{formatCurrency(receipt.baseAmount)}</Typography>
                </Box>
                <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%" }}>
                  <Typography>Descuento</Typography>
                  <Typography color="secondary.dark">
                    -{formatCurrency(receipt.discountAmount)}
                  </Typography>
                </Box>
                {receipt.discountDetails && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    {receipt.discountDetails}
                  </Typography>
                )}
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    width: "100%",
                    mt: 1,
                  }}
                >
                  <Typography variant="h6">Total pagado</Typography>
                  <Typography variant="h6" color="primary.dark">
                    {formatCurrency(receipt.finalAmount)}
                  </Typography>
                </Box>
              </Box>

              <Divider />

              <Box>
                <Typography variant="overline" color="text.secondary">
                  Pago
                </Typography>
                <Typography>
                  Pagado el {formatDateTime(receipt.paymentDate)}
                </Typography>
                {receipt.cardLastFour && (
                  <Typography color="text.secondary">
                    Tarjeta terminada en •••• {receipt.cardLastFour}
                  </Typography>
                )}
                <Typography color="text.secondary">
                  Estado: {receipt.paymentStatus}
                </Typography>
              </Box>
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => window.print()} disabled={!receipt}>
            Imprimir
          </Button>
          <Button onClick={handleCloseReceipt} variant="contained">
            Cerrar
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}

export default ReservationDetail;
