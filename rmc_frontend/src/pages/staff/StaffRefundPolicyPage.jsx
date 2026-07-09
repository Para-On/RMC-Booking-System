import { useEffect, useMemo, useState } from 'react'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
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
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { getRefundPolicy, updateRefundPolicy, updateSystemConfig } from '@/staffApi'

const DEFAULT_FORM = {
  name: 'Default hotel policy',
  enabled: true,
  fullCutoffValue: 48,
  fullCutoffUnit: 'HOURS',
  partialEnabled: true,
  partialRefundPercent: 50,
  deductionPercent: 50,
  checkInTime: '14:00',
  timezone: 'Asia/Manila',
  description: '',
}

function previewExample(form) {
  const checkIn = new Date()
  checkIn.setDate(checkIn.getDate() + 12)
  checkIn.setHours(parseInt(form.checkInTime.split(':')[0], 10), parseInt(form.checkInTime.split(':')[1], 10), 0, 0)

  const cutoff = new Date(checkIn)
  if (form.fullCutoffUnit === 'DAYS') {
    cutoff.setDate(cutoff.getDate() - form.fullCutoffValue)
  } else {
    cutoff.setHours(cutoff.getHours() - form.fullCutoffValue)
  }

  const paid = 10000
  const partialRefund = Math.round((paid * form.partialRefundPercent) / 100)

  return {
    checkIn: checkIn.toLocaleString(),
    cutoff: cutoff.toLocaleString(),
    fullRefund: paid,
    partialRefund,
    deduction: paid - partialRefund,
  }
}

export default function StaffRefundPolicyPage() {
  const [form, setForm] = useState(DEFAULT_FORM)
  const [manualRefundEnabled, setManualRefundEnabled] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [togglingManualRefund, setTogglingManualRefund] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const example = useMemo(() => previewExample(form), [form])

  useEffect(() => {
    loadPolicy()
  }, [])

  async function loadPolicy() {
    setLoading(true)
    setError('')
    try {
      const data = await getRefundPolicy()
      setManualRefundEnabled(data.manualRefundEnabled ?? false)
      setForm({
        name: data.name || DEFAULT_FORM.name,
        enabled: data.enabled ?? true,
        fullCutoffValue: data.fullCutoffValue ?? 48,
        fullCutoffUnit: data.fullCutoffUnit || 'HOURS',
        partialEnabled: data.partialEnabled ?? true,
        partialRefundPercent: data.partialRefundPercent ?? 50,
        deductionPercent: data.deductionPercent ?? 100 - (data.partialRefundPercent ?? 50),
        checkInTime: (data.checkInTime || '14:00').slice(0, 5),
        timezone: data.timezone || 'Asia/Manila',
        description: data.description || '',
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function updateField(field, value) {
    setForm((prev) => {
      const next = { ...prev, [field]: value }
      if (field === 'deductionPercent') {
        next.partialRefundPercent = Math.max(0, Math.min(100, 100 - Number(value)))
      }
      if (field === 'partialRefundPercent') {
        next.deductionPercent = Math.max(0, Math.min(100, 100 - Number(value)))
      }
      return next
    })
  }

  async function handleManualRefundToggle(nextEnabled) {
    setTogglingManualRefund(true)
    setError('')
    setMessage('')
    try {
      await updateSystemConfig('manualRefundEnabled', nextEnabled ? 'true' : 'false')
      setManualRefundEnabled(nextEnabled)
      setMessage(
        nextEnabled
          ? 'Manual refunds enabled. Managers can record off-platform refunds on booking details.'
          : 'Manual refunds disabled.',
      )
    } catch (err) {
      setError(err.message)
    } finally {
      setTogglingManualRefund(false)
    }
  }

  async function handleSave(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    setMessage('')
    try {
      await updateRefundPolicy({
        name: form.name,
        enabled: form.enabled,
        fullCutoffValue: Number(form.fullCutoffValue),
        fullCutoffUnit: form.fullCutoffUnit,
        partialEnabled: form.partialEnabled,
        partialRefundPercent: Number(form.partialRefundPercent),
        checkInTime: form.checkInTime,
        timezone: form.timezone,
        description: form.description || null,
      })
      setMessage('Refund policy saved. New bookings will use this policy; existing bookings keep their snapshot.')
      await loadPolicy()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <StaffPageShell title="Refund policy">
        <p className="text-sm text-muted-foreground">Loading policy…</p>
      </StaffPageShell>
    )
  }

  return (
    <StaffPageShell
      title="Refund policy"
      description="Configure cancellation cutoffs and partial refund rules. Policies are snapshotted when a booking is confirmed."
    >
      {error && <StaffAlert variant="destructive">{error}</StaffAlert>}
      {message && <StaffAlert>{message}</StaffAlert>}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Manual refunds</CardTitle>
          <CardDescription>
            Allow managers to record refunds paid outside Maya when the gateway cannot process a
            payout. Manual refunds count toward the policy cap and prevent duplicate Maya refunds.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Staff record manual refunds from each booking&apos;s detail page when this is on.
            </p>
            <div className="flex items-center gap-3">
              <Label htmlFor="manual-refund-enabled" className="text-sm">
                {manualRefundEnabled ? 'On' : 'Off'}
              </Label>
              <Switch
                id="manual-refund-enabled"
                checked={manualRefundEnabled}
                disabled={togglingManualRefund}
                onCheckedChange={handleManualRefundToggle}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      <form className="grid gap-6 lg:grid-cols-2" onSubmit={handleSave}>
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Policy settings</CardTitle>
            <CardDescription>
              Cutoff is measured from the scheduled check-in date and time ({form.timezone}).
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <Label htmlFor="policy-enabled">Refund policy enabled</Label>
                <p className="text-xs text-muted-foreground">Allow online guest cancellation under these rules.</p>
              </div>
              <Switch
                id="policy-enabled"
                checked={form.enabled}
                onCheckedChange={(value) => updateField('enabled', value)}
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="policy-name">Policy name</Label>
              <Input
                id="policy-name"
                value={form.name}
                onChange={(e) => updateField('name', e.target.value)}
                required
              />
            </div>

            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="full-cutoff">Full refund cutoff</Label>
                <Input
                  id="full-cutoff"
                  type="number"
                  min="0"
                  max="365"
                  value={form.fullCutoffValue}
                  onChange={(e) => updateField('fullCutoffValue', e.target.value)}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label>Cutoff unit</Label>
                <Select
                  value={form.fullCutoffUnit}
                  onValueChange={(value) => updateField('fullCutoffUnit', value)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="HOURS">Hours</SelectItem>
                    <SelectItem value="DAYS">Days</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="check-in-time">Standard check-in time</Label>
                <Input
                  id="check-in-time"
                  type="time"
                  value={form.checkInTime}
                  onChange={(e) => updateField('checkInTime', e.target.value)}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="timezone">Timezone</Label>
                <Input
                  id="timezone"
                  value={form.timezone}
                  onChange={(e) => updateField('timezone', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="flex items-center justify-between gap-4">
              <div>
                <Label htmlFor="partial-enabled">Partial refund enabled</Label>
                <p className="text-xs text-muted-foreground">
                  Allow cancellation inside the cutoff with a reduced refund.
                </p>
              </div>
              <Switch
                id="partial-enabled"
                checked={form.partialEnabled}
                onCheckedChange={(value) => updateField('partialEnabled', value)}
              />
            </div>

            {form.partialEnabled && (
              <div className="grid gap-2 sm:grid-cols-2">
                <div className="grid gap-2">
                  <Label htmlFor="deduction-percent">Deduction percentage</Label>
                  <Input
                    id="deduction-percent"
                    type="number"
                    min="0"
                    max="100"
                    value={form.deductionPercent}
                    onChange={(e) => updateField('deductionPercent', e.target.value)}
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="refund-percent">Refund percentage</Label>
                  <Input
                    id="refund-percent"
                    type="number"
                    min="0"
                    max="100"
                    value={form.partialRefundPercent}
                    onChange={(e) => updateField('partialRefundPercent', e.target.value)}
                  />
                </div>
              </div>
            )}

            <div className="grid gap-2">
              <Label htmlFor="description">Policy description (shown to guests)</Label>
              <Textarea
                id="description"
                rows={4}
                value={form.description}
                onChange={(e) => updateField('description', e.target.value)}
                placeholder="Free cancellation until 48 hours before check-in…"
              />
            </div>

            <Button type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save refund policy'}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Example scenario</CardTitle>
            <CardDescription>
              Illustration using ₱10,000 paid and check-in about 12 days from now.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <p>
              <strong>Check-in:</strong> {example.checkIn}
            </p>
            <p>
              <strong>Full refund until:</strong> {example.cutoff}
            </p>
            <p>
              Cancel <em>before</em> cutoff → <strong>100% refund (₱{example.fullRefund.toLocaleString()})</strong>
            </p>
            {form.partialEnabled ? (
              <p>
                Cancel <em>on or after</em> cutoff →{' '}
                <strong>
                  {form.deductionPercent}% deduction (₱{example.deduction.toLocaleString()}), {form.partialRefundPercent}%
                  refund (₱{example.partialRefund.toLocaleString()})
                </strong>
              </p>
            ) : (
              <p>Cancel on or after cutoff → <strong>not allowed</strong></p>
            )}
            <p className="text-muted-foreground">
              Existing confirmed bookings keep the policy that was active when they were confirmed.
            </p>
          </CardContent>
        </Card>
      </form>
    </StaffPageShell>
  )
}
