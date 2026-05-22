import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import {
  Alert,
  Button,
  Container,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { Link as RouterLink, useParams } from "react-router-dom";
import paymentService from "../services/payment.service";
import reservationService from "../services/reservation.service";
import { formatCurrency, formatDateTime } from "../utils/formatters.js";
import { validatePaymentForm } from "../utils/paymentValidation.js";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import ReservationSummary from "./ReservationSummary.jsx";

const initialPaymentForm = {
  cardHolderName: "",
  cardNumber: "",
  expirationDate: "",
  cvv: "",
};

function Payment() {
  const { reservationId } = useParams();
  const [reservation, setReservation] = useState(null);
  const [payment, setPayment] = useState(null);
  const [form, setForm] = useState(initialPaymentForm);
  const [formErrors, setFormErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

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

  const updateField = (field) => (event) => {
    setForm((currentForm) => ({
      ...currentForm,
      [field]: event.target.value,
    }));
  };

  // Auto-formatea MM/YY mientras el usuario escribe (agrega "/" después del mes)
  const updateExpirationDate = (event) => {
    let value = event.target.value.replace(/[^\d]/g, "");
    if (value.length > 2) {
      value = value.slice(0, 2) + "/" + value.slice(2, 4);
    }
    setForm((currentForm) => ({ ...currentForm, expirationDate: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const errors = validatePaymentForm(form);
    setFormErrors(errors);

    if (Object.keys(errors).length > 0) {
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const cleanCardNumber = form.cardNumber.replace(/\s/g, "");
      const response = await paymentService.process({
        reservationId: Number(reservationId),
        cardLastFour: cleanCardNumber.slice(-4),
        cardHolderName: form.cardHolderName.trim(),
      });
      setPayment(response.data);
      const updatedResponse = await reservationService.getById(reservationId);
      setReservation(updatedResponse.data);
      setForm(initialPaymentForm);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo procesar el pago.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <LoadingState message="Cargando pago..." />;
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Button
          component={RouterLink}
          to={`/reservations/${reservationId}`}
          startIcon={<ArrowBackIcon />}
          sx={{ alignSelf: "flex-start" }}
        >
          Volver a la reserva
        </Button>

        <ErrorAlert message={error} />

        {payment ? (
          <Alert severity="success" icon={<CreditCardIcon />}>
            Pago confirmado por {formatCurrency(payment.amount)} el{" "}
            {formatDateTime(payment.paidAt)}. Tarjeta terminada en {payment.cardLastFour}.
          </Alert>
        ) : null}

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 7 }}>
            {reservation ? <ReservationSummary reservation={reservation} /> : null}
          </Grid>
          <Grid size={{ xs: 12, md: 5 }}>
            <Paper component="form" onSubmit={handleSubmit} sx={{ p: 3 }}>
              <Stack spacing={2}>
                <Typography variant="h5" component="h1">
                  Pago simulado
                </Typography>
                <Typography color="text.secondary">
                  Se registrará un pago total. No se almacenará el número completo de
                  tarjeta.
                </Typography>

                <TextField
                  label="Nombre del titular"
                  value={form.cardHolderName}
                  onChange={updateField("cardHolderName")}
                  error={Boolean(formErrors.cardHolderName)}
                  helperText={formErrors.cardHolderName}
                  fullWidth
                />
                <TextField
                  label="Número de tarjeta simulada"
                  value={form.cardNumber}
                  onChange={updateField("cardNumber")}
                  error={Boolean(formErrors.cardNumber)}
                  helperText={formErrors.cardNumber}
                  fullWidth
                />
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="Expiración"
                      placeholder="MM/AA"
                      value={form.expirationDate}
                      onChange={updateExpirationDate}
                      inputProps={{ maxLength: 5 }}
                      error={Boolean(formErrors.expirationDate)}
                      helperText={formErrors.expirationDate}
                      fullWidth
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="CVV"
                      value={form.cvv}
                      onChange={updateField("cvv")}
                      error={Boolean(formErrors.cvv)}
                      helperText={formErrors.cvv}
                      fullWidth
                    />
                  </Grid>
                </Grid>

                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={submitting || reservation?.status !== "PENDING_PAYMENT"}
                >
                  Confirmar pago total
                </Button>
              </Stack>
            </Paper>
          </Grid>
        </Grid>
      </Stack>
    </Container>
  );
}

export default Payment;
