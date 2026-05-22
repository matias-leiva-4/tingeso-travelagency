import { Button, Container, Paper, Stack, Typography } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";

function NotFound() {
  return (
    <Container maxWidth="sm" sx={{ py: 8 }}>
      <Paper sx={{ p: 4, textAlign: "center" }}>
        <Stack spacing={2}>
          <Typography variant="h3" component="h1">
            Ruta no encontrada
          </Typography>
          <Typography color="text.secondary">
            La sección que buscas no existe o fue movida.
          </Typography>
          <Button component={RouterLink} to="/packages" variant="contained">
            Volver al catálogo
          </Button>
        </Stack>
      </Paper>
    </Container>
  );
}

export default NotFound;
