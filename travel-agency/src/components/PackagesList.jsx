import { Container, Grid, Stack, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import packageService from "../services/package.service";
import EmptyState from "./EmptyState.jsx";
import ErrorAlert from "./ErrorAlert.jsx";
import LoadingState from "./LoadingState.jsx";
import PackageCard from "./PackageCard.jsx";
import PackageSearchFilters from "./PackageSearchFilters.jsx";

const initialFilters = {
  destination: "",
  minPrice: "",
  maxPrice: "",
  startDate: "",
  endDate: "",
  packageType: "",
};

function PackagesList() {
  const [filters, setFilters] = useState(initialFilters);
  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState("");

  const loadAvailablePackages = async () => {
    setError("");
    setLoading(true);
    try {
      const response = await packageService.getAll();
      setPackages(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo cargar los paquetes.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAvailablePackages();
  }, []);

  const handleSearch = async (event) => {
    event.preventDefault();
    setSearching(true);
    setError("");
    try {
      const response = await packageService.search(filters);
      setPackages(response.data);
    } catch (err) {
      setError(
        err?.response?.data?.message ?? err?.message ?? "No se pudo buscar paquetes.",
      );
    } finally {
      setSearching(false);
    }
  };

  const handleReset = async () => {
    setFilters(initialFilters);
    await loadAvailablePackages();
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom fontWeight={700}>
        Paquetes disponibles
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Compara destinos, fechas, cupos y precios vigentes antes de reservar.
      </Typography>

      <Stack spacing={3}>
        <PackageSearchFilters
          filters={filters}
          onChange={setFilters}
          onSubmit={handleSearch}
          onReset={handleReset}
          searching={searching}
        />

        <ErrorAlert message={error} />

        {loading ? (
          <LoadingState message="Buscando paquetes disponibles..." />
        ) : packages.length === 0 ? (
          <EmptyState
            title="No encontramos paquetes"
            description="Ajusta los filtros o vuelve a revisar cuando la agencia publique nuevas fechas."
          />
        ) : (
          <>
            <Typography color="text.secondary">
              {packages.length} paquetes encontrados
            </Typography>
            <Grid container spacing={3}>
              {packages.map((packageItem) => (
                <Grid key={packageItem.id} size={{ xs: 12, md: 6, lg: 4 }}>
                  <PackageCard packageItem={packageItem} />
                </Grid>
              ))}
            </Grid>
          </>
        )}
      </Stack>
    </Container>
  );
}

export default PackagesList;
