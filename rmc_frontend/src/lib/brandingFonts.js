export const BRANDING_FONT_OPTIONS = [
  {
    value: 'Geist Variable',
    label: 'Geist (default)',
    cssFamily: "'Geist Variable', sans-serif",
    googleFonts: null,
  },
  {
    value: 'Inter',
    label: 'Inter',
    cssFamily: "'Inter', sans-serif",
    googleFonts: 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap',
  },
  {
    value: 'DM Sans',
    label: 'DM Sans',
    cssFamily: "'DM Sans', sans-serif",
    googleFonts: 'https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap',
  },
  {
    value: 'Plus Jakarta Sans',
    label: 'Plus Jakarta Sans',
    cssFamily: "'Plus Jakarta Sans', sans-serif",
    googleFonts:
      'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap',
  },
]

export function fontOptionForFamily(fontFamily) {
  return BRANDING_FONT_OPTIONS.find((option) => option.value === fontFamily) || BRANDING_FONT_OPTIONS[0]
}
