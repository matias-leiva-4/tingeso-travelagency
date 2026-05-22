export function formatRut(value) {
  const sanitized = String(value ?? '')
    .replace(/[^0-9kK]/g, '')
    .toUpperCase()

  if (!sanitized) {
    return ''
  }

  if (sanitized.length === 1) {
    return sanitized
  }

  const verifier = sanitized.slice(-1)
  const body = sanitized.slice(0, -1)
  const formattedBody = body.replace(/\B(?=(\d{3})+(?!\d))/g, '.')

  return `${formattedBody}-${verifier}`
}

export function formatCurrency(value) {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0))
}

export function formatDate(value) {
  if (!value) {
    return 'Sin fecha'
  }

  return new Intl.DateTimeFormat('es-CL', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
  }).format(new Date(`${value}T00:00:00`))
}

export function formatDateTime(value) {
  if (!value) {
    return 'Sin fecha'
  }

  return new Intl.DateTimeFormat('es-CL', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

const statusLabels = {
  AVAILABLE: 'Disponible',
  SOLD_OUT: 'Agotado',
  EXPIRED: 'No vigente',
  CANCELLED: 'Cancelado',
  PENDING_PAYMENT: 'Pendiente de pago',
  CONFIRMED: 'Confirmada',
  COMPLETED: 'Completado',
  FAILED: 'Fallido',
}

export function formatStatus(status) {
  return statusLabels[status] ?? status ?? 'Sin estado'
}
