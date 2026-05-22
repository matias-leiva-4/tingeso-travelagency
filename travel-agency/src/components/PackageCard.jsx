import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import EventSeatIcon from '@mui/icons-material/EventSeat'
import {
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardMedia,
  Chip,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { formatCurrency, formatDate, formatStatus } from '../utils/formatters.js'
import { getPackageImage } from '../utils/packageImages.js'
import { StatusChip } from './StatusChip.jsx'

export function PackageCard({ packageItem }) {
  const canReserve = packageItem.status === 'AVAILABLE' && packageItem.availableSlots > 0

  return (
    <Card sx={{
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      overflow: 'hidden',
      opacity: canReserve ? 1 : 0.6,
      filter: canReserve ? 'none' : 'grayscale(50%)',
      transition: 'opacity 0.2s, filter 0.2s',
    }}>
      <CardMedia
        component="img"
        image={getPackageImage(packageItem)}
        alt={`Destino ${packageItem.destination}`}
        height="180"
        sx={{ objectFit: 'cover' }}
      />
      <CardContent sx={{ flexGrow: 1 }}>
        <Stack spacing={1.5}>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
            <Box>
              <Typography variant="overline" color="primary.dark">
                {packageItem.destination}
              </Typography>
              <Typography variant="h5" component="h2">
                {packageItem.name}
              </Typography>
            </Box>
            <StatusChip status={packageItem.status} />
          </Stack>

          <Typography color="text.secondary" sx={{ minHeight: 48 }}>
            {packageItem.description}
          </Typography>

          <Stack spacing={1}>
            <Stack direction="row" spacing={1} alignItems="center" color="text.secondary">
              <CalendarMonthIcon fontSize="small" />
              <Typography variant="body2">
                {formatDate(packageItem.startDate)} al {formatDate(packageItem.endDate)}
              </Typography>
            </Stack>
            <Stack direction="row" spacing={1} alignItems="center" color="text.secondary">
              <EventSeatIcon fontSize="small" />
              <Typography variant="body2">{packageItem.availableSlots} cupos disponibles</Typography>
            </Stack>
          </Stack>

          <Typography variant="h5" component="p" color="primary.dark">
            {formatCurrency(packageItem.price)}
          </Typography>
        </Stack>
      </CardContent>
      <CardActions sx={{ p: 2, pt: 0 }}>
        <Button fullWidth component={RouterLink} to={`/packages/${packageItem.id}`} variant="outlined">
          Ver detalle
        </Button>
        {canReserve ? (
          <Button
            fullWidth
            component={RouterLink}
            to={`/checkout/${packageItem.id}`}
            variant="contained"
          >
            Reservar
          </Button>
        ) : (
          <Chip
            label={packageItem.status === 'SOLD_OUT' ? 'Agotado' : formatStatus(packageItem.status)}
            color="default"
            variant="outlined"
            sx={{ width: '100%' }}
          />
        )}
      </CardActions>
    </Card>
  )
}

export default PackageCard
