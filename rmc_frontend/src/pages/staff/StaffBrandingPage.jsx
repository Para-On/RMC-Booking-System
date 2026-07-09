import { useEffect, useMemo, useState } from 'react'
import { BrandMark, BRAND_LOGO_IMAGE_CLASS, BRAND_LOGO_SLOT_CLASS } from '@/components/branding/BrandMark'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useBranding } from '@/context/BrandingProvider'
import { applyBranding, DEFAULT_BRANDING } from '@/lib/applyBranding'
import { resolveBrandingPalette } from '@/lib/brandingColors'
import { BRANDING_FONT_OPTIONS } from '@/lib/brandingFonts'
import { getStaffBranding, updateStaffBranding, uploadBrandingLogo } from '@/staffApi'

const COLOR_FIELDS = [
  {
    key: 'primaryColor',
    label: 'Primary',
    description: 'Buttons, links, accents, and highlights',
  },
  {
    key: 'secondaryColor',
    label: 'Secondary',
    description: 'Page backgrounds, guest header/footer, and surfaces',
  },
]

function ColorField({ field, value, onChange }) {
  return (
    <div className="space-y-2">
      <div>
        <Label htmlFor={field.key}>{field.label}</Label>
        <p className="text-xs text-muted-foreground">{field.description}</p>
      </div>
      <div className="flex items-center gap-2">
        <Input
          id={field.key}
          type="color"
          value={value}
          onChange={(e) => onChange(field.key, e.target.value)}
          className="h-10 w-14 shrink-0 cursor-pointer p-1"
        />
        <Input
          value={value}
          onChange={(e) => onChange(field.key, e.target.value)}
          pattern="^#[0-9A-Fa-f]{6}$"
          className="font-mono uppercase"
        />
      </div>
    </div>
  )
}

function buildSavePayload(form) {
  return {
    fontFamily: form.fontFamily,
    primaryColor: form.primaryColor,
    secondaryColor: form.secondaryColor,
    footerText: form.footerText || null,
    footerContactEmail: form.footerContactEmail || null,
    footerContactPhone: form.footerContactPhone || null,
    footerCopyright: form.footerCopyright || null,
  }
}

export default function StaffBrandingPage() {
  const { refreshBranding } = useBranding()
  const [form, setForm] = useState(DEFAULT_BRANDING)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploadingLogo, setUploadingLogo] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const palette = useMemo(() => resolveBrandingPalette(form), [form])

  useEffect(() => {
    loadBranding()
  }, [])

  useEffect(() => {
    applyBranding(form)
  }, [form])

  async function loadBranding() {
    setLoading(true)
    setError('')
    try {
      const data = await getStaffBranding()
      setForm({
        ...DEFAULT_BRANDING,
        ...data,
        primaryColor: data.primaryColor || DEFAULT_BRANDING.primaryColor,
        secondaryColor: data.secondaryColor || DEFAULT_BRANDING.secondaryColor,
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSave(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const saved = await updateStaffBranding(buildSavePayload(form))
      const merged = {
        ...form,
        ...saved,
        primaryColor: saved.primaryColor || form.primaryColor,
        secondaryColor: saved.secondaryColor || form.secondaryColor,
      }
      setForm(merged)
      applyBranding(merged)
      await refreshBranding()
      setMessage('Branding saved')
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleLogoUpload(e) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploadingLogo(true)
    setError('')
    try {
      const result = await uploadBrandingLogo(file)
      updateField('logoUrl', result.assetUrl)
      await refreshBranding()
      setMessage('Logo uploaded')
    } catch (err) {
      setError(err.message)
    } finally {
      setUploadingLogo(false)
      e.target.value = ''
    }
  }

  function resetDefaults() {
    setForm(DEFAULT_BRANDING)
    setMessage('Reset to defaults — save to apply permanently')
  }

  if (loading) {
    return (
      <StaffPage title="Branding">
        <p className="text-sm text-muted-foreground">Loading branding settings…</p>
      </StaffPage>
    )
  }

  return (
    <StaffPage
      title="Branding"
      description="Upload your logo, choose a font, and set primary and secondary brand colors."
      actions={
        <Button type="button" variant="outline" onClick={resetDefaults}>
          Reset preview
        </Button>
      }
    >
      {error && <StaffAlert>{error}</StaffAlert>}
      {message && <StaffAlert variant="success">{message}</StaffAlert>}

      <form className="grid gap-6 xl:grid-cols-[1fr_20rem]" onSubmit={handleSave}>
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Logo</CardTitle>
              <CardDescription>Shown in the guest header and staff sidebar.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {form.logoUrl ? (
                <div className={`${BRAND_LOGO_SLOT_CLASS} rounded border bg-white p-1`}>
                  <img
                    src={form.logoUrl}
                    alt="Logo preview"
                    className={BRAND_LOGO_IMAGE_CLASS}
                  />
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No logo uploaded yet.</p>
              )}
              <Input
                type="file"
                accept="image/png,image/jpeg,image/webp,image/svg+xml"
                onChange={handleLogoUpload}
                disabled={uploadingLogo}
              />
              <p className="text-xs text-muted-foreground">PNG, JPG, WebP, or SVG up to 5 MB</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Typography</CardTitle>
              <CardDescription>Font used across guest and staff interfaces.</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="max-w-sm space-y-2">
                <Label>Font family</Label>
                <Select value={form.fontFamily} onValueChange={(value) => updateField('fontFamily', value)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {BRANDING_FONT_OPTIONS.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Brand colors</CardTitle>
              <CardDescription>
                Primary drives buttons, links, and accents. Secondary drives backgrounds and guest
                chrome. Borders and focus styles follow the active light/dark theme automatically.
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              {COLOR_FIELDS.map((field) => (
                <ColorField
                  key={field.key}
                  field={field}
                  value={form[field.key]}
                  onChange={updateField}
                />
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Guest footer</CardTitle>
              <CardDescription>
                Shown on every guest page. Leave blank to use sensible defaults.
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2 sm:col-span-2">
                <Label htmlFor="footerText">Footer message</Label>
                <Input
                  id="footerText"
                  value={form.footerText || ''}
                  onChange={(e) => updateField('footerText', e.target.value)}
                  placeholder="Book your stay with confidence."
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="footerContactEmail">Contact email</Label>
                <Input
                  id="footerContactEmail"
                  type="email"
                  value={form.footerContactEmail || ''}
                  onChange={(e) => updateField('footerContactEmail', e.target.value)}
                  placeholder="hello@hotel.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="footerContactPhone">Contact phone</Label>
                <Input
                  id="footerContactPhone"
                  value={form.footerContactPhone || ''}
                  onChange={(e) => updateField('footerContactPhone', e.target.value)}
                  placeholder="+63 900 000 0000"
                />
              </div>
              <div className="space-y-2 sm:col-span-2">
                <Label htmlFor="footerCopyright">Copyright line</Label>
                <Input
                  id="footerCopyright"
                  value={form.footerCopyright || ''}
                  onChange={(e) => updateField('footerCopyright', e.target.value)}
                  placeholder="© 2026 RMC Booking. All rights reserved."
                />
              </div>
            </CardContent>
          </Card>

          <div className="flex justify-end">
            <Button type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save branding'}
            </Button>
          </div>
        </div>

        <Card className="h-fit xl:sticky xl:top-20">
          <CardHeader>
            <CardTitle>Live preview</CardTitle>
            <CardDescription>Primary and secondary across buttons, links, and chrome.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div
              className="overflow-hidden rounded-lg"
              style={{
                backgroundColor: palette.headerBackgroundColor,
                color: palette.headerForegroundColor,
              }}
            >
              <div className="flex items-center justify-between px-4 py-2.5 text-sm">
                <span className="font-semibold">Header preview</span>
                <span className="opacity-80">Book · Lookup · Staff</span>
              </div>
            </div>
            <div className="rounded-lg bg-card p-4 shadow-sm">
              <div className="flex items-center">
                <BrandMark />
              </div>
              <p className="mt-3 text-sm text-muted-foreground">Sample booking card copy</p>
              <div className="mt-4 flex flex-wrap gap-2">
                <Button type="button">Primary</Button>
                <Button type="button" variant="secondary">
                  Secondary
                </Button>
                <Button type="button" variant="outline">
                  Outline
                </Button>
              </div>
              <div className="mt-3 flex flex-wrap gap-2">
                <Badge>Accent badge</Badge>
                <Badge variant="outline">Outline badge</Badge>
              </div>
              <p className="mt-3 text-sm">
                <span className="text-primary underline">Highlighted link</span>
              </p>
              <Input className="mt-3" placeholder="Focused input preview" readOnly />
            </div>
            <div
              className="overflow-hidden rounded-lg px-4 py-3 text-sm"
              style={{
                backgroundColor: palette.footerBackgroundColor,
                color: palette.footerForegroundColor,
              }}
            >
              <p className="font-semibold">Footer preview</p>
              <p className="mt-1 opacity-80">
                {form.footerText?.trim() || 'Book your stay with confidence.'}
              </p>
            </div>
          </CardContent>
        </Card>
      </form>
    </StaffPage>
  )
}
