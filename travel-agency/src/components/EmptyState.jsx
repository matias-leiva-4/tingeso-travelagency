import TravelExploreIcon from '@mui/icons-material/TravelExplore'
import { Paper, Stack, Typography } from '@mui/material'

export function EmptyState({ title = 'No hay resultados', description = 'Prueba con otros filtros.' }) {
  return (
    <Paper sx={{ p: 4, textAlign: 'center' }}>
      <Stack spacing={1.5} alignItems="center">
        <TravelExploreIcon color="primary" fontSize="large" />
        <Typography variant="h5" component="h2">
          {title}
        </Typography>
        <Typography color="text.secondary">{description}</Typography>
      </Stack>
    </Paper>
  )
}

export default EmptyState
