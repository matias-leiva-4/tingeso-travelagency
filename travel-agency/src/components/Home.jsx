import FlightTakeoffIcon from "@mui/icons-material/FlightTakeoff";
import { Box, Button, Container, Typography } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";

function Home() {
  return (
    <Container maxWidth="md" sx={{ py: 8, textAlign: "center" }}>
      <Box sx={{ mb: 4 }}>
        <FlightTakeoffIcon color="primary" sx={{ fontSize: 80 }} />
      </Box>
      <Typography variant="h3" component="h1" fontWeight={800} gutterBottom>
        Bienvenido a TravelAgency
      </Typography>
      <Typography variant="h6" color="text.secondary" sx={{ mb: 4 }}>
        Descubre los mejores destinos del mundo. Reserva tu viaje hoy y vive
        experiencias inolvidables con nuestros paquetes exclusivos.
      </Typography>
      <Button
        component={RouterLink}
        to="/packages"
        variant="contained"
        size="large"
      >
        Ver paquetes disponibles
      </Button>
    </Container>
  );
}

export default Home;
