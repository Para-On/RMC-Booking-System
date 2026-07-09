function hexToRgb(hex) {
  const normalized = hex.replace('#', '')
  const value = parseInt(normalized, 16)
  if (Number.isNaN(value)) return [26, 26, 26]
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255]
}

function relativeLuminance([r, g, b]) {
  const channels = [r, g, b].map((channel) => {
    const normalized = channel / 255
    return normalized <= 0.03928
      ? normalized / 12.92
      : ((normalized + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
}

export function contrastingForeground(hex) {
  return relativeLuminance(hexToRgb(hex)) > 0.55 ? '#1a1a1a' : '#fafafa'
}

export function resolveBrandingPalette(branding) {
  const primaryColor = branding?.primaryColor || '#1a1a1a'
  const secondaryColor = branding?.secondaryColor || '#f4f4f5'
  const primaryForegroundColor = contrastingForeground(primaryColor)
  const secondaryForegroundColor = contrastingForeground(secondaryColor)

  return {
    primaryColor,
    secondaryColor,
    primaryForegroundColor,
    secondaryForegroundColor,
    accentColor: primaryColor,
    accentForegroundColor: primaryForegroundColor,
    headerBackgroundColor: secondaryColor,
    headerForegroundColor: secondaryForegroundColor,
    footerBackgroundColor: secondaryColor,
    footerForegroundColor: secondaryForegroundColor,
  }
}
