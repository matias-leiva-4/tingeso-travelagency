import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import EventSeatIcon from "@mui/icons-material/EventSeat";
import {
  Box,
  Button,
  Container,
  Divider,
  Grid,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { Link as RouterLink, useParams } from "react-router-dom";
import packageService from "../services/package.service";
import { formatCurrency, formatDate } from "../utils/formatters.js";
import { getPackageImage } from "../utils/packageImages.js";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import StatusChip from "./StatusChip.jsx";

function PackageDetail() {
  const { packageId } = useParams();
  const [packageItem, setPackageItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadPackage = async () => {
      setLoading(true);
      setError("");
      try {
        const response = await packageService.getById(packageId);
        setPackageItem(response.data);
      } catch (err) {
        setError(
          err?.response?.data?.message ?? err?.message ?? "No se pudo cargar el paquete.",
        );
      } finally {
        setLoading(false);
      }
    };

    loadPackage();
  }, [packageId]);

  if (loading) {
    return <LoadingState message="Cargando detalle del paquete..." />;
  }

  if (error) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <ErrorAlert message={error} />
      </Container>
    );
  }

  if (!packageItem) {
    return null;
  }

  const canReserve = packageItem.status === "AVAILABLE" && packageItem.availableSlots > 0;

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Button
          component={RouterLink}
          to="/packages"
          startIcon={<ArrowBackIcon />}
          sx={{ alignSelf: "flex-start" }}
        >
          Volver al catálogo
        </Button>

        <Paper sx={{ overflow: "hidden" }}>
          <Grid container>
            <Grid size={{ xs: 12, md: 6 }}>
              <Box
                component="img"
                src={getPackageImage(packageItem)}
                alt={`Destino ${packageItem.destination}`}
                sx={{
                  width: "100%",
                  height: "100%",
                  minHeight: 360,
                  objectFit: "cover",
                  display: "block",
                }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <Stack spacing={2.5} sx={{ p: { xs: 3, md: 4 } }}>
                <Stack direction="row" justifyContent="space-between" spacing={2}>
                  <Box>
                    <Typography variant="overline" color="primary.dark">
                      {packageItem.destination}
                    </Typography>
                    <Typography variant="h3" component="h1">
                      {packageItem.name}
                    </Typography>
                  </Box>
                  <StatusChip status={packageItem.status} />
                </Stack>

                <Typography color="text.secondary">{packageItem.description}</Typography>

                <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <CalendarMonthIcon color="primary" />
                    <Typography>
                      {formatDate(packageItem.startDate)} al {formatDate(packageItem.endDate)}
                    </Typography>
                  </Stack>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <EventSeatIcon color="primary" />
                    <Typography>{packageItem.availableSlots} cupos disponibles</Typography>
                  </Stack>
                </Stack>

                <Typography variant="h4" color="primary.dark">
                  {formatCurrency(packageItem.price)}
                </Typography>

                <Divider />

                <Box>
                  <Typography variant="h6" gutterBottom>
                    Servicios incluidos
                  </Typography>
                  <Typography color="text.secondary">
                    {packageItem.includedServices ||
                      "La agencia no registró servicios incluidos para este paquete."}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="h6" gutterBottom>
                    Restricciones
                  </Typography>
                  <Typography color="text.secondary">
                    {packageItem.restrictions || "No hay restricciones registradas."}
                  </Typography>
                </Box>

                <Button
                  component={RouterLink}
                  to={`/checkout/${packageItem.id}`}
                  variant="contained"
                  size="large"
                  disabled={!canReserve}
                >
                  {canReserve ? "Reservar este paquete" : "No disponible para reserva"}
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </Paper>
      </Stack>
    </Container>
  );
}

export default PackageDetail;
