import { Alert } from '@mui/material'

export function ErrorAlert({ message }) {
  if (!message) {
    return null
  }

  return <Alert severity="error">{message}</Alert>
}

export default ErrorAlert
