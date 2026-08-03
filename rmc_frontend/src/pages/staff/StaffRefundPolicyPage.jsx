import { useEffect, useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import { StaffModal } from '@/components/staff/StaffModal'
import {
  StaffTable,
  StaffTableAction,
  StaffTableActionSeparator,
  StaffTableActionsCell,
  StaffTableActionsHead,
  StaffTableBody,
  StaffTableCell,
  StaffTableHead,
  StaffTableHeader,
  StaffTablePanel,
  StaffTableRow,
  StaffTableRowActions,
  StaffTableWrap,
} from '@/components/staff/StaffTable'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
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
import {
  createRefundPolicy,
  deactivateRefundPolicy,
  getManagerConfig,
  listRefundPolicies,
  updateRefundPolicy,
  updateSystemConfig,
} from '@/staffApi'

const EMPTY_FORM = {
  name: '',
  enabled: true,
  fullCutoffValue: 48,
  fullCutoffUnit: 'HOURS',
  partialEnabled: true,
  partialRefundPercent: 50,
  nightsDeductionEnabled: false,
  nightsDeducted: 1,
  checkInTime: '14:00',
  timezone: 'Asia/Manila',
  description: '',
  refundable: true,
}

function Field({ label, children }) {
  return (
    <div>
      <Label className="mb-2 block text-sm">{label}</Label>
      {children}
    </div>
  )
}

function formFromPolicy(policy) {
  if (!policy) return { ...EMPTY_FORM }
  return {
    name: policy.name || '',
    enabled: policy.enabled !== false,
    fullCutoffValue: policy.fullCutoffValue ?? 48,
    fullCutoffUnit: policy.fullCutoffUnit || 'HOURS',
    partialEnabled: policy.partialEnabled !== false,
    partialRefundPercent: policy.partialRefundPercent ?? 50,
    nightsDeductionEnabled: Boolean(policy.nightsDeductionEnabled),
    nightsDeducted: policy.nightsDeducted ?? 1,
    checkInTime: (policy.checkInTime || '14:00').slice(0, 5),
    timezone: policy.timezone || 'Asia/Manila',
    description: policy.description || '',
    refundable: policy.refundable !== false,
  }
}

function buildPayload(form) {
  return {
    name: form.name.trim(),
    enabled: Boolean(form.enabled),
    fullCutoffValue: Number(form.fullCutoffValue) || 0,
    fullCutoffUnit: form.fullCutoffUnit,
    partialEnabled: Boolean(form.partialEnabled),
    partialRefundPercent: Number(form.partialRefundPercent) || 0,
    nightsDeductionEnabled: Boolean(form.nightsDeductionEnabled),
    nightsDeducted: Number(form.nightsDeducted) || 1,
    checkInTime: form.checkInTime,
    timezone: form.timezone,
    description: form.description?.trim() || null,
    refundable: Boolean(form.refundable),
  }
}

export default function StaffRefundPolicyPage() {
  const [policies, setPolicies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [editorOpen, setEditorOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [manualRefundEnabled, setManualRefundEnabled] = useState(false)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [list, config] = await Promise.all([
        listRefundPolicies(),
        getManagerConfig().catch(() => null),
      ])
      setPolicies(list)
      const manualItem = config?.systemConfig?.find((item) => item.key === 'manualRefundEnabled')
      if (manualItem != null) {
        setManualRefundEnabled(String(manualItem.value).toLowerCase() === 'true')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function openCreate() {
    setEditing(null)
    setForm({ ...EMPTY_FORM, name: 'New policy' })
    setEditorOpen(true)
  }

  function openEdit(policy) {
    setEditing(policy)
    setForm(formFromPolicy(policy))
    setEditorOpen(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.name.trim()) {
      setError('Policy name is required.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const payload = buildPayload(form)
      if (editing) {
        await updateRefundPolicy(editing.id, payload)
        setMessage(`Policy "${payload.name}" updated. Existing booking snapshots are unchanged.`)
      } else {
        await createRefundPolicy(payload)
        setMessage(`Policy "${payload.name}" created.`)
      }
      setEditorOpen(false)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDeactivate(policy) {
    if (!window.confirm(`Deactivate policy "${policy.name}"? Active rate plans must be reassigned first.`)) return
    setError('')
    try {
      await deactivateRefundPolicy(policy.id)
      setMessage(`Policy "${policy.name}" deactivated.`)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function toggleManualRefund(value) {
    setError('')
    try {
      await updateSystemConfig('manualRefundEnabled', value ? 'true' : 'false')
      setManualRefundEnabled(value)
      setMessage('Manual refund setting updated.')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <StaffPage
      title="Refund policy"
      description="Named cancel/refund policies. Rate plans select a policy from a dropdown. Late cancel may stack nights retained then a partial % of the remainder."
      actions={
        <Button type="button" size="sm" onClick={openCreate}>
          <Plus className="h-3.5 w-3.5" />
          Add policy
        </Button>
      }
    >
      {message && <StaffAlert variant="success">{message}</StaffAlert>}
      {error && !editorOpen && <StaffAlert>{error}</StaffAlert>}

      <div className="mb-6 flex items-center justify-between gap-4 rounded-lg border p-4">
        <div>
          <Label htmlFor="manual-refund">Manual refund methods</Label>
          <p className="text-xs text-muted-foreground">Allow staff to record GCash / bank / cash refunds when enabled.</p>
        </div>
        <Switch id="manual-refund" checked={manualRefundEnabled} onCheckedChange={toggleManualRefund} />
      </div>

      {loading && <p className="text-sm text-muted-foreground">Loading policies…</p>}
      {!loading && policies.length === 0 && (
        <p className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">
          No policies yet. Add a policy, then attach it when creating a rate plan.
        </p>
      )}
      {!loading && policies.length > 0 && (
        <StaffTablePanel>
          <StaffTableWrap>
            <StaffTable>
              <StaffTableHeader>
                <StaffTableRow>
                  <StaffTableHead>Name</StaffTableHead>
                  <StaffTableHead className="hidden sm:table-cell">Cutoff</StaffTableHead>
                  <StaffTableHead className="hidden md:table-cell">Partial / nights</StaffTableHead>
                  <StaffTableHead>Refundable</StaffTableHead>
                  <StaffTableHead>Online cancel</StaffTableHead>
                  <StaffTableActionsHead />
                </StaffTableRow>
              </StaffTableHeader>
              <StaffTableBody>
                {policies.map((policy) => (
                  <StaffTableRow key={policy.id}>
                    <StaffTableCell className="font-medium">{policy.name}</StaffTableCell>
                    <StaffTableCell className="hidden sm:table-cell text-muted-foreground">
                      {policy.fullCutoffValue} {policy.fullCutoffUnit === 'DAYS' ? 'days' : 'hours'}
                    </StaffTableCell>
                    <StaffTableCell className="hidden md:table-cell text-muted-foreground">
                      {policy.partialEnabled ? `${policy.partialRefundPercent}%` : '—'}
                      {policy.nightsDeductionEnabled ? ` · ${policy.nightsDeducted} night(s)` : ''}
                    </StaffTableCell>
                    <StaffTableCell>
                      <Badge variant={policy.refundable ? 'default' : 'secondary'} className="text-xs">
                        {policy.refundable ? 'Yes' : 'No'}
                      </Badge>
                    </StaffTableCell>
                    <StaffTableCell>
                      <Badge variant={policy.enabled ? 'outline' : 'secondary'} className="text-xs">
                        {policy.enabled ? 'Enabled' : 'Off'}
                      </Badge>
                    </StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions label={`Actions for ${policy.name}`}>
                        <StaffTableAction icon={Pencil} onClick={() => openEdit(policy)}>
                          Edit
                        </StaffTableAction>
                        <StaffTableActionSeparator />
                        <StaffTableAction
                          icon={Trash2}
                          variant="destructive"
                          onClick={() => handleDeactivate(policy)}
                        >
                          Deactivate
                        </StaffTableAction>
                      </StaffTableRowActions>
                    </StaffTableActionsCell>
                  </StaffTableRow>
                ))}
              </StaffTableBody>
            </StaffTable>
          </StaffTableWrap>
        </StaffTablePanel>
      )}

      <StaffModal
        open={editorOpen}
        onOpenChange={setEditorOpen}
        title={editing ? `Edit policy — ${editing.name}` : 'Add policy'}
        description="Changes apply to new bookings via rate plans that select this policy. Existing booking snapshots are not rewritten."
        size="lg"
        footer={
          <>
            <Button type="button" variant="outline" size="sm" onClick={() => setEditorOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="refund-policy-form" size="sm" disabled={saving}>
              {saving ? 'Saving…' : editing ? 'Save changes' : 'Create policy'}
            </Button>
          </>
        }
      >
        {error && editorOpen && <StaffAlert>{error}</StaffAlert>}
        <form id="refund-policy-form" className="space-y-5" onSubmit={handleSubmit}>
          <Field label="Policy name">
            <Input value={form.name} onChange={(e) => updateField('name', e.target.value)} required />
          </Field>
          <div className="flex items-center justify-between gap-4 rounded-lg border p-4">
            <div>
              <Label>Online cancellation enabled</Label>
              <p className="text-xs text-muted-foreground">Guests may cancel online under these rules.</p>
            </div>
            <Switch checked={form.enabled} onCheckedChange={(v) => updateField('enabled', v)} />
          </div>
          <div className="flex items-center justify-between gap-4 rounded-lg border p-4">
            <div>
              <Label>Refundable</Label>
              <p className="text-xs text-muted-foreground">Show refundable on guest offers using this policy.</p>
            </div>
            <Switch checked={form.refundable} onCheckedChange={(v) => updateField('refundable', v)} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Full refund cutoff">
              <Input
                type="number"
                min="0"
                value={form.fullCutoffValue}
                onChange={(e) => updateField('fullCutoffValue', e.target.value)}
              />
            </Field>
            <Field label="Cutoff unit">
              <Select value={form.fullCutoffUnit} onValueChange={(v) => updateField('fullCutoffUnit', v)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="HOURS">Hours</SelectItem>
                  <SelectItem value="DAYS">Days</SelectItem>
                </SelectContent>
              </Select>
            </Field>
            <Field label="Check-in time">
              <Input
                type="time"
                value={form.checkInTime}
                onChange={(e) => updateField('checkInTime', e.target.value)}
              />
            </Field>
            <Field label="Timezone">
              <Input value={form.timezone} onChange={(e) => updateField('timezone', e.target.value)} />
            </Field>
          </div>
          <div className="flex items-center justify-between gap-4 rounded-lg border p-4">
            <div>
              <Label>Partial refund %</Label>
              <p className="text-xs text-muted-foreground">After cutoff: apply % to remaining after nights fee (if any).</p>
            </div>
            <Switch checked={form.partialEnabled} onCheckedChange={(v) => updateField('partialEnabled', v)} />
          </div>
          {form.partialEnabled && (
            <Field label="Partial refund percentage">
              <Input
                type="number"
                min="0"
                max="100"
                value={form.partialRefundPercent}
                onChange={(e) => updateField('partialRefundPercent', e.target.value)}
              />
            </Field>
          )}
          <div className="flex items-center justify-between gap-4 rounded-lg border p-4">
            <div>
              <Label>Nights deduction</Label>
              <p className="text-xs text-muted-foreground">
                After cutoff: retain up to N nights (capped at stay length), then apply partial % to the rest.
              </p>
            </div>
            <Switch
              checked={form.nightsDeductionEnabled}
              onCheckedChange={(v) => updateField('nightsDeductionEnabled', v)}
            />
          </div>
          {form.nightsDeductionEnabled && (
            <Field label="Nights deducted">
              <Input
                type="number"
                min="1"
                max="30"
                value={form.nightsDeducted}
                onChange={(e) => updateField('nightsDeducted', e.target.value)}
              />
            </Field>
          )}
          <Field label="Policy description (shown to guests)">
            <Textarea
              rows={3}
              value={form.description}
              onChange={(e) => updateField('description', e.target.value)}
            />
          </Field>
        </form>
      </StaffModal>
    </StaffPage>
  )
}
