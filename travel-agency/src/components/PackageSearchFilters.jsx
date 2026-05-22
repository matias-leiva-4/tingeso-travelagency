import SearchIcon from '@mui/icons-material/Search'
import {
  Button,
  Grid,
  MenuItem,
  Paper,
  Stack,
  TextField,
} from '@mui/material'

const packageTypes = ['Aventura', 'Cultural', 'Familiar', 'Romántico', 'Relajo']

export function PackageSearchFilters({ filters, onChange, onSubmit, onReset, searching }) {
  const updateField = (field) => (event) => {
    onChange({
      ...filters,
      [field]: event.target.value,
    })
  }

  return (
    <Paper component="form" onSubmit={onSubmit} sx={{ p: 3 }}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <TextField
            fullWidth
            label="Destino"
            value={filters.destination}
            onChange={updateField('destination')}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2 }}>
          <TextField
            fullWidth
            type="number"
            label="Precio mínimo"
            value={filters.minPrice}
            onChange={updateField('minPrice')}
            slotProps={{ htmlInput: { min: 0 } }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2 }}>
          <TextField
            fullWidth
            type="number"
            label="Precio máximo"
            value={filters.maxPrice}
            onChange={updateField('maxPrice')}
            slotProps={{ htmlInput: { min: 0 } }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2 }}>
          <TextField
            fullWidth
            type="date"
            label="Desde"
            value={filters.startDate}
            onChange={updateField('startDate')}
            slotProps={{ inputLabel: { shrink: true } }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2 }}>
          <TextField
            fullWidth
            type="date"
            label="Hasta"
            value={filters.endDate}
            onChange={updateField('endDate')}
            slotProps={{ inputLabel: { shrink: true } }}
          />
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <TextField
            fullWidth
            select
            label="Tipo de experiencia"
            value={filters.packageType}
            onChange={updateField('packageType')}
          >
            <MenuItem value="">Todos</MenuItem>
            {packageTypes.map((type) => (
              <MenuItem key={type} value={type}>
                {type}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid size={{ xs: 12, md: 8 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} justifyContent="flex-end">
            <Button type="button" variant="text" onClick={onReset}>
              Limpiar filtros
            </Button>
            <Button type="submit" variant="contained" startIcon={<SearchIcon />} disabled={searching}>
              Buscar paquetes
            </Button>
          </Stack>
        </Grid>
      </Grid>
    </Paper>
  )
}

export default PackageSearchFilters
