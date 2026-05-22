/**
 * Valida la fecha de expiración de una tarjeta.
 * Formato esperado: MM/YY o MM/YYYY
 * La tarjeta no debe estar vencida (la fecha debe ser >= mes actual).
 * Retorna un string de error, o null si es válida.
 */
function validateExpirationDate(expirationDate) {
  const raw = expirationDate.trim()

  if (!raw) {
    return 'Ingresa la fecha de expiración.'
  }

  // Aceptar MM/YY o MM/YYYY
  if (!/^\d{2}\/(\d{2}|\d{4})$/.test(raw)) {
    return 'Usa el formato MM/AA (ej: 08/28).'
  }

  const [monthStr, yearStr] = raw.split('/')
  const month = parseInt(monthStr, 10)
  const year = yearStr.length === 2
    ? 2000 + parseInt(yearStr, 10)
    : parseInt(yearStr, 10)

  if (month < 1 || month > 12) {
    return 'El mes debe estar entre 01 y 12.'
  }

  // La tarjeta vence al final del mes indicado
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentMonth = now.getMonth() + 1 // getMonth() es base 0

  if (year < currentYear || (year === currentYear && month < currentMonth)) {
    return 'La tarjeta está vencida.'
  }

  return null
}

export function validatePaymentForm({ cardHolderName, cardNumber, expirationDate, cvv }) {
  const cleanCardNumber = cardNumber.replace(/\s/g, '')
  const errors = {}

  if (!cardHolderName.trim()) {
    errors.cardHolderName = 'Ingresa el nombre del titular.'
  }

  if (!/^\d{12,19}$/.test(cleanCardNumber)) {
    errors.cardNumber = 'Ingresa un número de tarjeta simulado de 12 a 19 dígitos.'
  }

  const expError = validateExpirationDate(expirationDate)
  if (expError) {
    errors.expirationDate = expError
  }

  if (!/^\d{3,4}$/.test(cvv)) {
    errors.cvv = 'El CVV debe tener 3 o 4 dígitos.'
  }

  return errors
}
