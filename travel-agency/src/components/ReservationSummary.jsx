import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import {
  Box,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { formatCurrency, formatDateTime } from '../utils/formatters.js'
import { StatusChip } from './StatusChip.jsx'

export function ReservationSummary({ reservation }) {
  return (
    <Paper sx={{ p: 3 }}>
      <Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={2}>
          <Box>
            <Typography variant="overline" color="primary.dark">
              Reserva #{reservation.id}
            </Typography>
            <Typography variant="h5" component="h2">
              {reservation.packageName}
            </Typography>
            <Typography color="text.secondary">{reservation.packageDestination}</Typography>
          </Box>
          <StatusChip status={reservation.status} />
        </Stack>

        <Divider />

        <Stack spacing={1}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', gap: 2 }}>
            <Typography color="text.secondary">Pasajeros</Typography>
            <Typography fontWeight={700}>{reservation.passengerCount}</Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', gap: 2 }}>
            <Typography color="text.secondary">Precio original</Typography>
            <Typography>{formatCurrency(reservation.baseAmount)}</Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', gap: 2 }}>
            <Typography color="text.secondary">Descuentos</Typography>
            <Typography color="secondary.dark">-{formatCurrency(reservation.discountAmount)}</Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', gap: 2 }}>
            <Typography variant="h6">Total a pagar</Typography>
            <Typography variant="h6" color="primary.dark">
              {formatCurrency(reservation.finalAmount)}
            </Typography>
          </Box>
        </Stack>

        {reservation.appliedDiscounts?.length ? (
          <List dense disablePadding>
            {reservation.appliedDiscounts.map((discount) => (
              <ListItem key={discount} disableGutters>
                <ListItemIcon sx={{ minWidth: 32 }}>
                  <CheckCircleIcon color="success" fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={discount} />
              </ListItem>
            ))}
          </List>
        ) : (
          <Typography color="text.secondary">No se aplicaron descuentos a esta reserva.</Typography>
        )}

        <Typography variant="body2" color="text.secondary">
          Puedes pagar hasta: {formatDateTime(reservation.expiresAt)}
        </Typography>
      </Stack>
    </Paper>
  )
}

export default ReservationSummary
