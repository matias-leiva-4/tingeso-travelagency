import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import PublishIcon from "@mui/icons-material/Publish";
import {
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import adminService from "../services/admin.service";
import { formatCurrency, formatDate } from "../utils/formatters.js";
import EmptyState from "./EmptyState.jsx";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import StatusChip from "./StatusChip.jsx";

function AdminPackages() {
  const navigate = useNavigate();
  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleteDialog, setDeleteDialog] = useState({ open: false, pkg: null });
  const [deleting, setDeleting] = useState(false);
  const [publishingId, setPublishingId] = useState(null);

  const loadPackages = async () => {
    setError("");
    setLoading(true);
    try {
      const response = await adminService.getAllPackages();
      setPackages(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ??
          err?.message ??
          "No se pudo cargar los paquetes.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPackages();
  }, []);

  const handleDeleteClick = (pkg) => {
    setDeleteDialog({ open: true, pkg });
  };

  const handleDeleteCancel = () => {
    setDeleteDialog({ open: false, pkg: null });
  };

  const handleDeleteConfirm = async () => {
    if (!deleteDialog.pkg) return;
    setDeleting(true);
    setError("");
    try {
      await adminService.deletePackage(deleteDialog.pkg.id);
      setDeleteDialog({ open: false, pkg: null });
      await loadPackages();
    } catch (err) {
      setError(
        err?.response?.data?.message ??
          err?.message ??
          "No se pudo eliminar el paquete.",
      );
      setDeleteDialog({ open: false, pkg: null });
    } finally {
      setDeleting(false);
    }
  };

  const handlePublish = async (pkg) => {
    setPublishingId(pkg.id);
    setError("");
    try {
      await adminService.publishPackage(pkg.id);
      await loadPackages();
    } catch (err) {
      setError(
        err?.response?.data?.message ??
          err?.message ??
          "No se pudo publicar el paquete.",
      );
    } finally {
      setPublishingId(null);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            width: "100%",
            gap: 2,
            flexWrap: "wrap",
          }}
        >
          <div>
            <Typography variant="h4" component="h1" fontWeight={700}>
              Gestionar Paquetes
            </Typography>
            <Typography color="text.secondary">
              Administra los paquetes turísticos de la agencia.
            </Typography>
          </div>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate("/admin/packages/new")}
          >
            Nuevo Paquete
          </Button>
        </Box>

        <ErrorAlert message={error} />

        {loading ? (
          <LoadingState message="Cargando paquetes..." />
        ) : packages.length === 0 ? (
          <EmptyState
            title="No hay paquetes"
            description="Crea un nuevo paquete turístico para comenzar."
          />
        ) : (
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Nombre</TableCell>
                  <TableCell>Destino</TableCell>
                  <TableCell>Fechas</TableCell>
                  <TableCell>Precio</TableCell>
                  <TableCell>Cupos</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {packages.map((pkg) => (
                  <TableRow key={pkg.id} hover>
                    <TableCell>{pkg.name}</TableCell>
                    <TableCell>{pkg.destination}</TableCell>
                    <TableCell>
                      {formatDate(pkg.startDate)} – {formatDate(pkg.endDate)}
                    </TableCell>
                    <TableCell>{formatCurrency(pkg.price)}</TableCell>
                    <TableCell>
                      {pkg.availableSlots}/{pkg.totalSlots}
                    </TableCell>
                    <TableCell>
                      <StatusChip status={pkg.status} />
                    </TableCell>
                    <TableCell align="right">
                      <IconButton
                        color="primary"
                        onClick={() =>
                          navigate(`/admin/packages/${pkg.id}/edit`)
                        }
                        title="Editar"
                      >
                        <EditIcon />
                      </IconButton>
                      {pkg.status === "CANCELLED" ? (
                        <IconButton
                          color="success"
                          onClick={() => handlePublish(pkg)}
                          disabled={publishingId === pkg.id}
                          title="Publicar (re-activar paquete cancelado)"
                        >
                          <PublishIcon />
                        </IconButton>
                      ) : (
                        <IconButton
                          color="error"
                          onClick={() => handleDeleteClick(pkg)}
                          title="Cancelar"
                        >
                          <DeleteIcon />
                        </IconButton>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Stack>

      <Dialog open={deleteDialog.open} onClose={handleDeleteCancel}>
        <DialogTitle>Confirmar eliminación</DialogTitle>
        <DialogContent>
          <DialogContentText>
            ¿Estás seguro de que deseas eliminar el paquete{" "}
            <strong>{deleteDialog.pkg?.name}</strong>? Esta acción no se puede
            deshacer.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={deleting}>
            Cancelar
          </Button>
          <Button
            onClick={handleDeleteConfirm}
            color="error"
            variant="contained"
            disabled={deleting}
          >
            Eliminar
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}

export default AdminPackages;
