import { Box, CircularProgress, Stack, Typography } from '@mui/material'

export function LoadingState({ message = 'Cargando información...' }) {
  return (
    <Box sx={{ py: 8 }}>
      <Stack spacing={2} alignItems="center">
        <CircularProgress />
        <Typography color="text.secondary">{message}</Typography>
      </Stack>
    </Box>
  )
}

export default LoadingState
