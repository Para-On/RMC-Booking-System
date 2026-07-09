import { useBranding } from '@/context/BrandingProvider'

/** Fixed logo frame — layout slots stay this size regardless of image aspect ratio. */
export const BRAND_LOGO_SLOT_CLASS =
  'inline-flex h-15 w-20 shrink-0 items-center justify-center overflow-hidden'

export const BRAND_LOGO_IMAGE_CLASS = 'h-full w-full object-contain object-center'

export function BrandMark({ className = '', slotClassName = '', imageClassName = BRAND_LOGO_IMAGE_CLASS }) {
  const { branding } = useBranding()

  if (!branding.logoUrl) {
    return null
  }

  return (
    <span className={`${BRAND_LOGO_SLOT_CLASS} ${slotClassName} ${className}`.trim()}>
      <img src={branding.logoUrl} alt="Company logo" className={imageClassName} />
    </span>
  )
}
