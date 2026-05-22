import { Chip } from '@mui/material'
import { formatStatus } from '../utils/formatters.js'

const statusColors = {
  AVAILABLE: 'success',
  SOLD_OUT: 'warning',
  EXPIRED: 'default',
  CANCELLED: 'error',
  PENDING_PAYMENT: 'warning',
  CONFIRMED: 'success',
  COMPLETED: 'success',
  FAILED: 'error',
}

export function StatusChip({ status }) {
  return (
    <Chip
      label={formatStatus(status)}
      color={statusColors[status] ?? 'default'}
      size="small"
      variant={status === 'EXPIRED' ? 'outlined' : 'filled'}
    />
  )
}

export default StatusChip
