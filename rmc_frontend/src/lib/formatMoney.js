export function formatMoney(amount, currency = 'PHP') {
  const value = Number(amount || 0)
  const symbol = currency === 'PHP' ? '₱' : currency
  return `${symbol}${value.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}
