import { describe, expect, it } from 'vitest'
import { validatePaymentForm } from './paymentValidation.js'

describe('validatePaymentForm', () => {
  it('rejects empty and malformed payment data', () => {
    const errors = validatePaymentForm({
      cardHolderName: '',
      cardNumber: '123',
      expirationDate: '',
      cvv: '12',
    })

    expect(errors).toEqual({
      cardHolderName: 'Ingresa el nombre del titular.',
      cardNumber: 'Ingresa un número de tarjeta simulado de 12 a 19 dígitos.',
      expirationDate: 'Ingresa la fecha de expiración.',
      cvv: 'El CVV debe tener 3 o 4 dígitos.',
    })
  })

  it('accepts valid simulated card data', () => {
    const errors = validatePaymentForm({
      cardHolderName: 'Ada Lovelace',
      cardNumber: '4111 1111 1111 1111',
      expirationDate: '12/28',
      cvv: '123',
    })

    expect(errors).toEqual({})
  })
})
