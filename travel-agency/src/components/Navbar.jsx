import FlightTakeoffIcon from "@mui/icons-material/FlightTakeoff";
import MenuIcon from "@mui/icons-material/Menu";
import { AppBar, Button, IconButton, Toolbar, Typography } from "@mui/material";
import { useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { Link as RouterLink } from "react-router-dom";
import Sidemenu from "./Sidemenu.jsx";

function Navbar() {
  const { keycloak } = useKeycloak();
  const [openMenu, setOpenMenu] = useState(false);

  const username =
    keycloak.tokenParsed?.preferred_username ??
    keycloak.tokenParsed?.given_name ??
    "Usuario";

  return (
    <>
      <AppBar position="static" color="primary">
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setOpenMenu(true)}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>

          <FlightTakeoffIcon sx={{ mr: 1 }} />
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 800 }}>
            TravelAgency
          </Typography>

          {keycloak.authenticated ? (
            <>
              <Typography variant="body2" sx={{ mr: 2, display: { xs: "none", sm: "block" } }}>
                {username}
              </Typography>
              <Button
                color="inherit"
                component={RouterLink}
                to="/profile"
                sx={{ mr: 1, display: { xs: "none", md: "inline-flex" } }}
              >
                Mi perfil
              </Button>
              <Button
                color="inherit"
                variant="outlined"
                onClick={() =>
                  keycloak.logout({ redirectUri: window.location.origin })
                }
              >
                Cerrar sesión
              </Button>
            </>
          ) : (
            <Button
              color="inherit"
              variant="outlined"
              onClick={() =>
                keycloak.login({
                  redirectUri: `${window.location.origin}${window.location.pathname}`,
                })
              }
            >
              Iniciar sesión
            </Button>
          )}
        </Toolbar>
      </AppBar>

      <Sidemenu open={openMenu} onClose={() => setOpenMenu(false)} />
    </>
  );
}

export default Navbar;
