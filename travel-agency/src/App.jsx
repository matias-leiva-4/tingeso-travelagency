import { Alert, Container, Typography } from "@mui/material";
import { useKeycloak } from "@react-keycloak/web";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import AdminPackages from "./components/AdminPackages.jsx";
import AdminReports from "./components/AdminReports.jsx";
import Home from "./components/Home.jsx";
import MyReservations from "./components/MyReservations.jsx";
import Navbar from "./components/Navbar.jsx";
import NotFound from "./components/NotFound.jsx";
import PackageDetail from "./components/PackageDetail.jsx";
import PackageForm from "./components/PackageForm.jsx";
import PackagesList from "./components/PackagesList.jsx";
import Payment from "./components/Payment.jsx";
import ReservationCheckout from "./components/ReservationCheckout.jsx";
import ReservationDetail from "./components/ReservationDetail.jsx";
import UserProfile from "./components/UserProfile.jsx";

function PrivateRoute({ element }) {
  const { keycloak } = useKeycloak();
  if (!keycloak.authenticated) {
    keycloak.login({
      redirectUri: `${window.location.origin}${window.location.pathname}`,
    });
    return null;
  }
  return element;
}

function AdminRoute({ element }) {
  const { keycloak } = useKeycloak();
  if (!keycloak.authenticated) {
    keycloak.login({
      redirectUri: `${window.location.origin}${window.location.pathname}`,
    });
    return null;
  }
  if (!keycloak.hasRealmRole("ADMIN")) {
    return (
      <Container maxWidth="sm" sx={{ py: 8 }}>
        <Alert severity="error">
          <Typography variant="h6" gutterBottom>
            Acceso denegado
          </Typography>
          <Typography>
            No tienes permisos de administrador para acceder a esta sección.
          </Typography>
        </Alert>
      </Container>
    );
  }
  return element;
}

function App() {
  const { initialized } = useKeycloak();

  if (!initialized) {
    return <div>Cargando...</div>;
  }

  return (
    <Router>
      <div className="container">
        <Navbar />
        <Routes>
          <Route path="/" element={<PackagesList />} />
          <Route path="/home" element={<Home />} />
          <Route path="/packages" element={<PackagesList />} />
          <Route path="/packages/:packageId" element={<PackageDetail />} />
          <Route
            path="/checkout/:packageId"
            element={<PrivateRoute element={<ReservationCheckout />} />}
          />
          <Route
            path="/reservations/:reservationId"
            element={<PrivateRoute element={<ReservationDetail />} />}
          />
          <Route
            path="/payments/:reservationId"
            element={<PrivateRoute element={<Payment />} />}
          />
          <Route
            path="/my-reservations"
            element={<PrivateRoute element={<MyReservations />} />}
          />
          <Route
            path="/profile"
            element={<PrivateRoute element={<UserProfile />} />}
          />
          <Route
            path="/admin/packages"
            element={<AdminRoute element={<AdminPackages />} />}
          />
          <Route
            path="/admin/packages/new"
            element={<AdminRoute element={<PackageForm />} />}
          />
          <Route
            path="/admin/packages/:id/edit"
            element={<AdminRoute element={<PackageForm />} />}
          />
          <Route
            path="/admin/reports"
            element={<AdminRoute element={<AdminReports />} />}
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
