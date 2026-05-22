import SearchIcon from "@mui/icons-material/Search";
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Grid,
  Paper,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";
import adminService from "../services/admin.service";
import { formatCurrency, formatDateTime } from "../utils/formatters.js";
import EmptyState from "./EmptyState.jsx";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import StatusChip from "./StatusChip.jsx";

function AdminReports() {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [activeTab, setActiveTab] = useState(0);

  const [salesReport, setSalesReport] = useState(null);
  const [ranking, setRanking] = useState(null);

  const [loadingSales, setLoadingSales] = useState(false);
  const [loadingRanking, setLoadingRanking] = useState(false);
  const [errorSales, setErrorSales] = useState("");
  const [errorRanking, setErrorRanking] = useState("");

  const handleGenerate = async () => {
    if (!from || !to) return;

    setErrorSales("");
    setErrorRanking("");
    setLoadingSales(true);
    setLoadingRanking(true);

    try {
      const response = await adminService.getSalesReport(from, to);
      setSalesReport(response.data);
    } catch (err) {
      setErrorSales(
        err?.response?.data?.message ??
          err?.message ??
          "No se pudo cargar el reporte de ventas.",
      );
    } finally {
      setLoadingSales(false);
    }

    try {
      const response = await adminService.getPackageRanking(from, to);
      setRanking(response.data);
    } catch (err) {
      setErrorRanking(
        err?.response?.data?.message ??
          err?.message ??
          "No se pudo cargar el ranking de paquetes.",
      );
    } finally {
      setLoadingRanking(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <div>
          <Typography variant="h4" component="h1" fontWeight={700}>
            Reportes
          </Typography>
          <Typography color="text.secondary">
            Consulta las ventas y el rendimiento de los paquetes turísticos.
          </Typography>
        </div>

        <Paper sx={{ p: 3 }}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems="center">
            <TextField
              type="date"
              label="Desde"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              size="small"
            />
            <TextField
              type="date"
              label="Hasta"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              size="small"
            />
            <Button
              variant="contained"
              startIcon={<SearchIcon />}
              onClick={handleGenerate}
              disabled={!from || !to || loadingSales || loadingRanking}
            >
              Generar
            </Button>
          </Stack>
        </Paper>

        <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
          <Tabs value={activeTab} onChange={(_, value) => setActiveTab(value)}>
            <Tab label="Ventas por período" />
            <Tab label="Ranking de paquetes" />
          </Tabs>
        </Box>

        {activeTab === 0 && (
          <SalesTab
            report={salesReport}
            loading={loadingSales}
            error={errorSales}
          />
        )}

        {activeTab === 1 && (
          <RankingTab
            ranking={ranking}
            loading={loadingRanking}
            error={errorRanking}
          />
        )}
      </Stack>
    </Container>
  );
}

function SalesTab({ report, loading, error }) {
  if (loading) {
    return <LoadingState message="Cargando reporte de ventas..." />;
  }

  if (error) {
    return <ErrorAlert message={error} />;
  }

  if (!report) {
    return (
      <EmptyState
        title="Sin datos"
        description="Selecciona un rango de fechas y presiona Generar para ver el reporte."
      />
    );
  }

  const reservations = report.reservations ?? [];

  return (
    <Stack spacing={3}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" variant="body2">
                Total reservas
              </Typography>
              <Typography variant="h4" fontWeight={700}>
                {report.totalReservations ?? 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" variant="body2">
                Total pasajeros
              </Typography>
              <Typography variant="h4" fontWeight={700}>
                {report.totalPassengers ?? 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" variant="body2">
                Ingresos totales
              </Typography>
              <Typography variant="h4" fontWeight={700}>
                {formatCurrency(report.totalRevenue)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {reservations.length === 0 ? (
        <EmptyState
          title="Sin reservas"
          description="No se encontraron reservas en el período seleccionado."
        />
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Fecha</TableCell>
                <TableCell>Cliente</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Paquete</TableCell>
                <TableCell>Destino</TableCell>
                <TableCell align="right">Pasajeros</TableCell>
                <TableCell align="right">Monto Base</TableCell>
                <TableCell align="right">Descuento</TableCell>
                <TableCell align="right">Monto Final</TableCell>
                <TableCell>Estado</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {reservations.map((row, index) => (
                <TableRow key={row.reservationId ?? index} hover>
                  <TableCell>{formatDateTime(row.reservationDate)}</TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>{row.clientEmail}</TableCell>
                  <TableCell>{row.packageName}</TableCell>
                  <TableCell>{row.destination}</TableCell>
                  <TableCell align="right">{row.passengerCount}</TableCell>
                  <TableCell align="right">
                    {formatCurrency(row.baseAmount)}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(row.discountAmount)}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(row.finalAmount)}
                  </TableCell>
                  <TableCell>
                    <StatusChip status={row.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Stack>
  );
}

function RankingTab({ ranking, loading, error }) {
  if (loading) {
    return <LoadingState message="Cargando ranking de paquetes..." />;
  }

  if (error) {
    return <ErrorAlert message={error} />;
  }

  if (!ranking) {
    return (
      <EmptyState
        title="Sin datos"
        description="Selecciona un rango de fechas y presiona Generar para ver el ranking."
      />
    );
  }

  if (ranking.length === 0) {
    return (
      <EmptyState
        title="Sin resultados"
        description="No se encontraron paquetes con reservas en el período seleccionado."
      />
    );
  }

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>#</TableCell>
            <TableCell>Paquete</TableCell>
            <TableCell>Destino</TableCell>
            <TableCell align="right">Reservas</TableCell>
            <TableCell align="right">Pasajeros</TableCell>
            <TableCell align="right">Ingresos Totales</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {ranking.map((row, index) => (
            <TableRow key={row.packageId ?? index} hover>
              <TableCell>{index + 1}</TableCell>
              <TableCell>{row.packageName}</TableCell>
              <TableCell>{row.destination}</TableCell>
              <TableCell align="right">{row.totalReservations}</TableCell>
              <TableCell align="right">{row.totalPassengers}</TableCell>
              <TableCell align="right">
                {formatCurrency(row.totalRevenue)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export default AdminReports;
