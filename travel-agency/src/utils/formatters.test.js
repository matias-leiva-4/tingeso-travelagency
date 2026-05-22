import { describe, expect, it } from 'vitest'
import { formatCurrency, formatDate, formatDateTime, formatRut, formatStatus } from './formatters.js'

describe('formatters', () => {
  it('formats currency in CLP', () => {
    expect(formatCurrency(15000)).toContain('15')
  })

  it('formats a date value', () => {
    expect(formatDate('2026-04-22')).toBeTruthy()
  })

  it('formats a datetime value', () => {
    expect(formatDateTime('2026-04-22T10:30:00')).toBeTruthy()
  })

  it('formats RUT values while typing', () => {
    expect(formatRut('12345678')).toBe('1.234.567-8')
    expect(formatRut('123456789')).toBe('12.345.678-9')
    expect(formatRut('12.345.678-k')).toBe('12.345.678-K')
  })

  it('formats known statuses', () => {
    expect(formatStatus('PENDING_PAYMENT')).toBe('Pendiente de pago')
  })
})
