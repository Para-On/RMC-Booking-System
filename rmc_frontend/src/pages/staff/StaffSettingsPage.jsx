import { useEffect, useState } from 'react'
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
import { Textarea } from '@/components/ui/textarea'
import {
  confirmMfaSetup,
  createRoomUnit,
  disableMfa,
  getConfigAuditLog,
  getManagerConfig,
  getRoomConfigOptions,
  startMfaSetup,
  updateDailyRates,
  updateRatePlanConfig,
  updateRoomTypeConfig,
  updateRoomUnitStatus,
  updateSystemConfig,
} from '@/staffApi'

export default function StaffSettingsPage() {
  const [config, setConfig] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [savingKey, setSavingKey] = useState('')
  const [newRoom, setNewRoom] = useState({ roomTypeId: '', roomNumber: '', floorLabel: '' })
  const [rateBatch, setRateBatch] = useState({})
  const [auditLog, setAuditLog] = useState([])
  const [statusOptions, setStatusOptions] = useState([])
  const [mfaSetup, setMfaSetup] = useState(null)
  const [mfaCode, setMfaCode] = useState('')

  useEffect(() => {
    loadConfig()
    loadAudit()
    getRoomConfigOptions()
      .then((data) => setStatusOptions(data.statuses || []))
      .catch(() => setStatusOptions([]))
  }, [])

  function findStatusOption(label) {
    return statusOptions.find((option) => option.label.toLowerCase() === label.toLowerCase())
  }

  async function loadConfig() {
    setLoading(true)
    setError('')
    try {
      const data = await getManagerConfig()
      setConfig(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function saveSystemConfig(key, value) {
    setSavingKey(`system-${key}`)
    setMessage('')
    setError('')
    try {
      const updated = await updateSystemConfig(key, value)
      setConfig((prev) => ({
        ...prev,
        systemConfig: prev.systemConfig.map((item) => (item.key === key ? updated : item)),
      }))
      setMessage(`Saved ${key}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function saveRatePlan(plan, field, rawValue) {
    setSavingKey(`rate-${plan.id}-${field}`)
    setMessage('')
    setError('')
    const payload = {
      [field]:
        field === 'active'
          ? rawValue === 'true' || rawValue === true
          : field === 'cancellationPolicy'
            ? rawValue
            : Number(rawValue),
    }
    try {
      const updated = await updateRatePlanConfig(plan.id, payload)
      setConfig((prev) => ({
        ...prev,
        ratePlans: prev.ratePlans.map((item) => (item.id === plan.id ? updated : item)),
      }))
      setMessage(`Saved rate plan ${plan.name}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function saveRoomType(roomType, field, rawValue) {
    setSavingKey(`room-${roomType.id}-${field}`)
    setMessage('')
    setError('')
    const payload = {
      [field]:
        field === 'active'
          ? rawValue === 'true' || rawValue === true
          : Number(rawValue),
    }
    try {
      const updated = await updateRoomTypeConfig(roomType.id, payload)
      setConfig((prev) => ({
        ...prev,
        roomTypes: prev.roomTypes.map((item) => (item.id === roomType.id ? updated : item)),
      }))
      setMessage(`Saved room type ${roomType.name}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function handleAddRoom(e) {
    e.preventDefault()
    setSavingKey('add-room')
    setMessage('')
    setError('')
    try {
      await createRoomUnit(
        Number(newRoom.roomTypeId),
        newRoom.roomNumber.trim(),
        newRoom.floorLabel.trim() || null
      )
      setNewRoom({ roomTypeId: '', roomNumber: '', floorLabel: '' })
      setMessage('Room unit added')
      await loadConfig()
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function handleDailyRates(plan) {
    const batch = rateBatch[plan.id] || {}
    if (!batch.fromDate || !batch.toDate || !batch.amount) {
      setError('Enter from date, to date, and amount for daily rates')
      return
    }
    setSavingKey(`rates-${plan.id}`)
    setMessage('')
    setError('')
    try {
      const count = await updateDailyRates(plan.id, batch.fromDate, batch.toDate, batch.amount)
      setMessage(`Updated ${count} daily rate(s) for ${plan.name}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function loadAudit() {
    try {
      const data = await getConfigAuditLog()
      setAuditLog(data || [])
    } catch {
      setAuditLog([])
    }
  }

  async function handleDeactivateUnit(unit) {
    const outOfOrder = findStatusOption('Out of order')
    if (!outOfOrder) {
      setError('Out of order status is not configured.')
      return
    }
    if (!window.confirm(`Mark room ${unit.roomNumber} as out of order?`)) return
    setSavingKey(`unit-${unit.id}`)
    setError('')
    try {
      await updateRoomUnitStatus(unit.id, outOfOrder.id)
      setMessage(`Room ${unit.roomNumber} deactivated`)
      await loadConfig()
      await loadAudit()
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function handleReactivateUnit(unit) {
    const available = findStatusOption('Available')
    if (!available) {
      setError('Available status is not configured.')
      return
    }
    setSavingKey(`unit-${unit.id}`)
    setError('')
    try {
      await updateRoomUnitStatus(unit.id, available.id)
      setMessage(`Room ${unit.roomNumber} reactivated`)
      await loadConfig()
      await loadAudit()
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function handleStartMfa() {
    setSavingKey('mfa-setup')
    setError('')
    try {
      const data = await startMfaSetup()
      setMfaSetup(data)
      setMessage('Scan the OTP URL in your authenticator app, then enter a code to confirm.')
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function handleConfirmMfa(e) {
    e.preventDefault()
    setSavingKey('mfa-confirm')
    setError('')
    try {
      await confirmMfaSetup(mfaCode)
      setMfaSetup(null)
      setMfaCode('')
      setMessage('MFA enabled for your account')
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function handleDisableMfa() {
    if (!window.confirm('Disable MFA for your account?')) return
    setSavingKey('mfa-disable')
    setError('')
    try {
      await disableMfa()
      setMfaSetup(null)
      setMessage('MFA disabled')
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  return (
    <StaffPageShell
      title="Manager settings"
      description="System configuration, rate plans, room inventory, and security."
    >
      <StaffAlert variant="success">{message}</StaffAlert>
      <StaffAlert>{error}</StaffAlert>
      {loading && <p className="text-sm text-muted-foreground">Loading settings…</p>}

      {!loading && config && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">System configuration</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              {config.systemConfig.map((item) => (
                <SystemConfigRow
                  key={item.key}
                  item={item}
                  saving={savingKey === `system-${item.key}`}
                  onSave={saveSystemConfig}
                />
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Rate plans</CardTitle>
            </CardHeader>
            <CardContent className="space-y-8">
              {config.ratePlans.map((plan) => (
                <div key={plan.id} className="space-y-4 border-b pb-8 last:border-0 last:pb-0">
                  <h3 className="font-medium">
                    {plan.name}{' '}
                    <span className="text-sm font-normal text-muted-foreground">({plan.roomTypeName})</span>
                  </h3>
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    <Field label="Hold TTL (minutes)">
                      <Input
                        type="number"
                        min="1"
                        defaultValue={plan.holdTtlMinutes}
                        onBlur={(e) => saveRatePlan(plan, 'holdTtlMinutes', e.target.value)}
                        disabled={savingKey === `rate-${plan.id}-holdTtlMinutes`}
                      />
                    </Field>
                    <Field label="Refund window (hours)">
                      <Input
                        type="number"
                        min="1"
                        defaultValue={plan.refundWindowHours}
                        onBlur={(e) => saveRatePlan(plan, 'refundWindowHours', e.target.value)}
                        disabled={savingKey === `rate-${plan.id}-refundWindowHours`}
                      />
                    </Field>
                    <Field label="Pay-later cutoff (hours)">
                      <Input
                        type="number"
                        min="0"
                        defaultValue={plan.payLaterCutoffHours ?? 0}
                        onBlur={(e) => saveRatePlan(plan, 'payLaterCutoffHours', e.target.value)}
                        disabled={savingKey === `rate-${plan.id}-payLaterCutoffHours`}
                      />
                    </Field>
                    <Field label="Active" className="sm:col-span-2 lg:col-span-1">
                      <Select
                        defaultValue={plan.active ? 'true' : 'false'}
                        onValueChange={(value) => saveRatePlan(plan, 'active', value)}
                        disabled={savingKey === `rate-${plan.id}-active`}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="true">Yes</SelectItem>
                          <SelectItem value="false">No</SelectItem>
                        </SelectContent>
                      </Select>
                    </Field>
                    <Field label="Cancellation policy" className="sm:col-span-2 lg:col-span-3">
                      <Textarea
                        rows={2}
                        defaultValue={plan.cancellationPolicy || ''}
                        onBlur={(e) => saveRatePlan(plan, 'cancellationPolicy', e.target.value)}
                        disabled={savingKey === `rate-${plan.id}-cancellationPolicy`}
                      />
                    </Field>
                  </div>
                  <div className="rounded-lg border p-4">
                    <h4 className="mb-3 text-sm font-medium">Daily rates (batch update)</h4>
                    <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
                      <Field label="From">
                        <Input
                          type="date"
                          value={rateBatch[plan.id]?.fromDate || ''}
                          onChange={(e) =>
                            setRateBatch((prev) => ({
                              ...prev,
                              [plan.id]: { ...prev[plan.id], fromDate: e.target.value },
                            }))
                          }
                        />
                      </Field>
                      <Field label="To">
                        <Input
                          type="date"
                          value={rateBatch[plan.id]?.toDate || ''}
                          onChange={(e) =>
                            setRateBatch((prev) => ({
                              ...prev,
                              [plan.id]: { ...prev[plan.id], toDate: e.target.value },
                            }))
                          }
                        />
                      </Field>
                      <Field label="Amount">
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          placeholder="Amount"
                          value={rateBatch[plan.id]?.amount || ''}
                          onChange={(e) =>
                            setRateBatch((prev) => ({
                              ...prev,
                              [plan.id]: { ...prev[plan.id], amount: e.target.value },
                            }))
                          }
                        />
                      </Field>
                      <Button
                        type="button"
                        disabled={savingKey === `rates-${plan.id}`}
                        onClick={() => handleDailyRates(plan)}
                      >
                        {savingKey === `rates-${plan.id}` ? 'Saving…' : 'Apply'}
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Room types</CardTitle>
            </CardHeader>
            <CardContent className="space-y-8">
              {config.roomTypes.map((roomType) => (
                <div key={roomType.id} className="space-y-4 border-b pb-8 last:border-0 last:pb-0">
                  <h3 className="font-medium">{roomType.name}</h3>
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <Field label="Overbooking buffer">
                      <Input
                        type="number"
                        min="0"
                        defaultValue={roomType.overbookingBuffer}
                        onBlur={(e) => saveRoomType(roomType, 'overbookingBuffer', e.target.value)}
                        disabled={savingKey === `room-${roomType.id}-overbookingBuffer`}
                      />
                    </Field>
                    <Field label="Min advance booking (hours)">
                      <Input
                        type="number"
                        min="0"
                        defaultValue={roomType.minAdvanceBookingHours}
                        onBlur={(e) =>
                          saveRoomType(roomType, 'minAdvanceBookingHours', e.target.value)
                        }
                        disabled={savingKey === `room-${roomType.id}-minAdvanceBookingHours`}
                      />
                    </Field>
                    <Field label="Max advance booking (days)">
                      <Input
                        type="number"
                        min="1"
                        defaultValue={roomType.maxAdvanceBookingDays}
                        onBlur={(e) =>
                          saveRoomType(roomType, 'maxAdvanceBookingDays', e.target.value)
                        }
                        disabled={savingKey === `room-${roomType.id}-maxAdvanceBookingDays`}
                      />
                    </Field>
                    <Field label="Active">
                      <Select
                        defaultValue={roomType.active ? 'true' : 'false'}
                        onValueChange={(value) => saveRoomType(roomType, 'active', value)}
                        disabled={savingKey === `room-${roomType.id}-active`}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="true">Yes</SelectItem>
                          <SelectItem value="false">No</SelectItem>
                        </SelectContent>
                      </Select>
                    </Field>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Room units</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <form
                className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
                onSubmit={(e) => {
                  if (!newRoom.roomTypeId) {
                    e.preventDefault()
                    setError('Select a room type')
                    return
                  }
                  handleAddRoom(e)
                }}
              >
                <Field label="Room type">
                  <Select
                    value={newRoom.roomTypeId || undefined}
                    onValueChange={(roomTypeId) => setNewRoom((r) => ({ ...r, roomTypeId }))}
                    required
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select room type" />
                    </SelectTrigger>
                    <SelectContent>
                      {config.roomTypes.map((rt) => (
                        <SelectItem key={rt.id} value={String(rt.id)}>
                          {rt.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field label="Room number">
                  <Input
                    value={newRoom.roomNumber}
                    onChange={(e) => setNewRoom((r) => ({ ...r, roomNumber: e.target.value }))}
                    required
                  />
                </Field>
                <Field label="Floor">
                  <Input
                    value={newRoom.floorLabel}
                    onChange={(e) => setNewRoom((r) => ({ ...r, floorLabel: e.target.value }))}
                  />
                </Field>
                <div className="flex items-end">
                  <Button type="submit" disabled={savingKey === 'add-room'}>
                    {savingKey === 'add-room' ? 'Adding…' : 'Add room'}
                  </Button>
                </div>
              </form>
              <ul className="space-y-3 text-sm">
                {(config.roomUnits || []).map((unit) => (
                  <li
                    key={unit.id}
                    className="flex flex-col gap-2 rounded-lg border p-3 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <span>
                      {unit.roomNumber} — {unit.roomTypeName} (floor {unit.floorLabel || '—'}) ·{' '}
                      {unit.statusLabel || unit.dayStatus}
                    </span>
                    <div className="flex gap-2">
                      {unit.dayStatus !== 'OUT_OF_ORDER' && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={savingKey === `unit-${unit.id}`}
                          onClick={() => handleDeactivateUnit(unit)}
                        >
                          Deactivate
                        </Button>
                      )}
                      {unit.dayStatus === 'OUT_OF_ORDER' && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={savingKey === `unit-${unit.id}`}
                          onClick={() => handleReactivateUnit(unit)}
                        >
                          Reactivate
                        </Button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Configuration audit</CardTitle>
            </CardHeader>
            <CardContent>
              {auditLog.length === 0 ? (
                <p className="text-sm text-muted-foreground">No configuration changes recorded yet.</p>
              ) : (
                <ul className="space-y-2 text-sm">
                  {auditLog.map((entry) => (
                    <li key={entry.id} className="border-b pb-2 last:border-0">
                      {entry.createdAt?.replace('T', ' ').slice(0, 19)} — {entry.entityType}{' '}
                      {entry.configKey}: {entry.oldValue || '—'} → {entry.newValue || '—'}
                      {entry.staffEmail ? ` (${entry.staffEmail})` : ''}
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Your MFA</CardTitle>
              <CardDescription>
                Protect your staff account with a TOTP authenticator app (Google Authenticator,
                Authy, etc.).
              </CardDescription>
            </CardHeader>
            <CardContent>
              {!mfaSetup ? (
                <div className="flex flex-wrap gap-2">
                  <Button type="button" disabled={savingKey === 'mfa-setup'} onClick={handleStartMfa}>
                    {savingKey === 'mfa-setup' ? 'Starting…' : 'Set up MFA'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    disabled={savingKey === 'mfa-disable'}
                    onClick={handleDisableMfa}
                  >
                    Disable MFA
                  </Button>
                </div>
              ) : (
                <form className="grid max-w-md gap-4" onSubmit={handleConfirmMfa}>
                  <p className="text-xs text-muted-foreground">
                    Secret: <code className="rounded bg-muted px-1">{mfaSetup.secret}</code>
                  </p>
                  <p className="break-all text-xs text-muted-foreground">OTP URL: {mfaSetup.otpAuthUrl}</p>
                  <Field label="Confirm with 6-digit code">
                    <Input
                      value={mfaCode}
                      onChange={(e) => setMfaCode(e.target.value)}
                      required
                      maxLength={6}
                    />
                  </Field>
                  <Button type="submit" disabled={savingKey === 'mfa-confirm'}>
                    {savingKey === 'mfa-confirm' ? 'Confirming…' : 'Enable MFA'}
                  </Button>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </StaffPageShell>
  )
}

function Field({ label, children, className }) {
  return (
    <div className={className}>
      <Label className="mb-2 block text-sm">{label}</Label>
      {children}
    </div>
  )
}

function SystemConfigRow({ item, saving, onSave }) {
  const [value, setValue] = useState(item.value)

  useEffect(() => {
    setValue(item.value)
  }, [item.value])

  return (
    <div className="grid gap-2">
      <Label>
        {item.key}
        {item.description && (
          <span className="ml-1 font-normal text-muted-foreground">— {item.description}</span>
        )}
      </Label>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Input value={value} onChange={(e) => setValue(e.target.value)} disabled={saving} />
        <Button type="button" disabled={saving} onClick={() => onSave(item.key, value)}>
          {saving ? 'Saving…' : 'Save'}
        </Button>
      </div>
    </div>
  )
}
