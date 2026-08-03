import { useEffect, useMemo, useState } from 'react'
import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import RoomTypeWizardStepper from '@/components/staff/RoomTypeWizardStepper'
import { StaffAlert } from '@/components/staff/StaffPageShell'
import { StaffModal } from '@/components/staff/StaffModal'
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
import {
  buildCreateRatePlanPayload,
  buildUpdateRatePlanPayload,
  catalogFromStaffRoom,
  emptyRatePlanForm,
  RATE_PLAN_STEP_CONTENT,
  RATE_PLAN_WIZARD_STEPS,
  ratePlanFormFromConfig,
  validateRatePlanWizardStep,
} from '@/lib/roomCatalog'
import { createRatePlan, listRefundPolicies, updateDailyRates, updateRatePlanConfig } from '@/staffApi'

function Field({ label, children, className }) {
  return (
    <div className={className}>
      <Label className="mb-2 block text-sm">{label}</Label>
      {children}
    </div>
  )
}

function WizardStepHeader({ step }) {
  const content = RATE_PLAN_STEP_CONTENT[step]
  if (!content) return null
  return (
    <div className="mt-4 shrink-0 space-y-2">
      <h3 className="text-sm font-medium">{content.title}</h3>
      <p className="text-sm text-muted-foreground">{content.description}</p>
    </div>
  )
}

/**
 * Create / edit rate plan: catalog → policy → details → pricing → preview (room-type card).
 */
export default function RatePlanManager({
  open,
  onOpenChange,
  onChanged,
  roomTypes = [],
  roomUnits = [],
  editingPlan = null,
  initialRoomTypeId = '',
}) {
  const isEdit = Boolean(editingPlan)
  const [step, setStep] = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState(emptyRatePlanForm())
  const [roomTypeId, setRoomTypeId] = useState('')
  const [policies, setPolicies] = useState([])
  const [rateBatch, setRateBatch] = useState({ fromDate: '', toDate: '', amount: '' })

  useEffect(() => {
    if (!open) return
    setError('')
    setStep(0)
    listRefundPolicies()
      .then(setPolicies)
      .catch(() => setPolicies([]))
    if (editingPlan) {
      setForm(ratePlanFormFromConfig(editingPlan))
      setRoomTypeId(editingPlan.roomTypeId != null ? String(editingPlan.roomTypeId) : '')
      setRateBatch({
        fromDate: '',
        toDate: '',
        amount: editingPlan.sampleNightlyRate != null ? String(editingPlan.sampleNightlyRate) : '',
      })
    } else {
      setForm(emptyRatePlanForm())
      setRoomTypeId(initialRoomTypeId ? String(initialRoomTypeId) : '')
      setRateBatch({ fromDate: '', toDate: '', amount: '' })
    }
  }, [open, editingPlan, initialRoomTypeId])

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const selectedRoom = useMemo(
    () => roomTypes.find((r) => String(r.id) === String(roomTypeId)),
    [roomTypes, roomTypeId]
  )

  const inheritedUnits = useMemo(() => {
    if (!roomTypeId) return []
    return roomUnits
      .filter((u) => String(u.roomTypeId) === String(roomTypeId))
      .map((u) => u.roomNumber)
      .sort((a, b) => String(a).localeCompare(String(b), undefined, { numeric: true }))
  }, [roomUnits, roomTypeId])

  const selectedPolicy = useMemo(
    () => policies.find((p) => String(p.id) === String(form.refundPolicyId)),
    [policies, form.refundPolicyId]
  )

  const previewCatalog = useMemo(() => {
    const base = selectedRoom
      ? catalogFromStaffRoom(selectedRoom)
      : {
          name: 'Select a room type',
          description: '',
          imageUrls: [],
          amenities: [],
          maxAdults: 2,
          maxChildren: 0,
        }
    return {
      ...base,
      fromPrice: form.baseNightlyRate
        ? Number(form.baseNightlyRate)
        : editingPlan?.sampleNightlyRate != null
          ? Number(editingPlan.sampleNightlyRate)
          : null,
      totalPrice: form.baseNightlyRate
        ? Number(form.baseNightlyRate)
        : editingPlan?.sampleNightlyRate != null
          ? Number(editingPlan.sampleNightlyRate)
          : null,
      refundable: selectedPolicy?.refundable ?? editingPlan?.refundable,
      freeCancellation: selectedPolicy?.refundable !== false,
      showFrom: true,
    }
  }, [selectedRoom, form.baseNightlyRate, selectedPolicy, editingPlan])

  function goNext() {
    const err = validateRatePlanWizardStep(step, { form, roomTypeId, isEdit })
    if (err) {
      setError(err)
      return
    }
    setError('')
    setStep((s) => Math.min(s + 1, RATE_PLAN_WIZARD_STEPS.length - 1))
  }

  function goBack() {
    setError('')
    setStep((s) => Math.max(s - 1, 0))
  }

  async function handleSave() {
    for (let s = 0; s < RATE_PLAN_WIZARD_STEPS.length - 1; s++) {
      const err = validateRatePlanWizardStep(s, { form, roomTypeId, isEdit })
      if (err) {
        setError(err)
        setStep(s)
        return
      }
    }
    setSaving(true)
    setError('')
    try {
      if (isEdit) {
        await updateRatePlanConfig(editingPlan.id, buildUpdateRatePlanPayload(form))
        if (rateBatch.fromDate && rateBatch.toDate && rateBatch.amount) {
          await updateDailyRates(editingPlan.id, {
            fromDate: rateBatch.fromDate,
            toDate: rateBatch.toDate,
            amount: Number(rateBatch.amount),
          })
        }
        onChanged?.('Rate plan updated.')
      } else {
        await createRatePlan(Number(roomTypeId), buildCreateRatePlanPayload(form))
        onChanged?.('Rate plan created.')
      }
      onOpenChange(false)
    } catch (err) {
      setError(err.message || 'Failed to save rate plan')
    } finally {
      setSaving(false)
    }
  }

  const lastStep = RATE_PLAN_WIZARD_STEPS.length - 1

  return (
    <StaffModal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? 'Edit rate plan' : 'Create rate plan'}
      description={`Step ${step + 1} of ${RATE_PLAN_WIZARD_STEPS.length} — policy and pricing under a room type.`}
      size="wizard"
      bodyClassName="flex min-h-0 flex-1 flex-col overflow-hidden py-3 sm:py-4"
      footer={
        <>
          <Button type="button" variant="outline" size="sm" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          {step > 0 && (
            <Button type="button" variant="outline" size="sm" onClick={goBack}>
              Back
            </Button>
          )}
          {step < lastStep ? (
            <Button type="button" size="sm" onClick={goNext}>
              Next
            </Button>
          ) : (
            <Button type="button" size="sm" disabled={saving} onClick={handleSave}>
              {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create rate plan'}
            </Button>
          )}
        </>
      }
    >
      <div className="flex h-full min-h-0 flex-col">
        <div className="shrink-0 overflow-visible">
          <RoomTypeWizardStepper steps={RATE_PLAN_WIZARD_STEPS} currentStep={step} />
        </div>
        {error && <StaffAlert className="mb-3 shrink-0">{error}</StaffAlert>}
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain pr-0.5">
          {step === 0 && (
            <section className="space-y-4">
              <WizardStepHeader step={0} />
              <Field label="Room type">
                <Select
                  value={roomTypeId || undefined}
                  onValueChange={setRoomTypeId}
                  disabled={isEdit}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select room type" />
                  </SelectTrigger>
                  <SelectContent>
                    {roomTypes.map((rt) => (
                      <SelectItem key={rt.id} value={String(rt.id)}>
                        {rt.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>
              {roomTypeId && (
                <div className="rounded-lg border bg-muted/20 px-3 py-2 text-sm">
                  <p className="font-medium">Inherited room numbers</p>
                  <p className="mt-1 text-muted-foreground">
                    {inheritedUnits.length
                      ? inheritedUnits.join(', ')
                      : 'No room numbers assigned to this type yet.'}
                  </p>
                </div>
              )}
            </section>
          )}

          {step === 1 && (
            <section className="space-y-4">
              <WizardStepHeader step={1} />
              <Field label="Refund policy">
                <Select
                  value={form.refundPolicyId || undefined}
                  onValueChange={(v) => updateField('refundPolicyId', v)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select policy" />
                  </SelectTrigger>
                  <SelectContent>
                    {policies.map((p) => (
                      <SelectItem key={p.id} value={String(p.id)}>
                        {p.name}
                        {p.refundable === false ? ' (non-refundable)' : ''}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>
              {selectedPolicy && (
                <p className="text-sm text-muted-foreground">
                  {selectedPolicy.description ||
                    (selectedPolicy.refundable
                      ? `Full cancel cutoff: ${selectedPolicy.fullCutoffValue ?? '—'} ${selectedPolicy.fullCutoffUnit || 'HOURS'}`
                      : 'Non-refundable')}
                </p>
              )}
            </section>
          )}

          {step === 2 && (
            <section className="space-y-4">
              <WizardStepHeader step={2} />
              <Field label="Plan name">
                <Input value={form.name} onChange={(e) => updateField('name', e.target.value)} />
              </Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Hold TTL (minutes)">
                  <Input
                    type="number"
                    min={1}
                    value={form.holdTtlMinutes}
                    onChange={(e) => updateField('holdTtlMinutes', e.target.value)}
                  />
                </Field>
                <Field label="Pay-later cutoff (hours)">
                  <Input
                    type="number"
                    min={0}
                    value={form.payLaterCutoffHours}
                    onChange={(e) => updateField('payLaterCutoffHours', e.target.value)}
                  />
                </Field>
              </div>
              <div className="flex items-center justify-between rounded-lg border px-3 py-2">
                <div>
                  <p className="text-sm font-medium">Active</p>
                  <p className="text-xs text-muted-foreground">Inactive plans are hidden from guests.</p>
                </div>
                <Switch checked={form.active} onCheckedChange={(v) => updateField('active', v)} />
              </div>
            </section>
          )}

          {step === 3 && (
            <section className="space-y-4">
              <WizardStepHeader step={3} />
              {!isEdit ? (
                <Field label="Base nightly rate (PHP)">
                  <Input
                    type="number"
                    min={0.01}
                    step="0.01"
                    value={form.baseNightlyRate}
                    onChange={(e) => updateField('baseNightlyRate', e.target.value)}
                  />
                </Field>
              ) : (
                <div className="space-y-3 rounded-lg border p-3">
                  <p className="text-sm font-medium">Update daily rates (optional)</p>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <Field label="From">
                      <Input
                        type="date"
                        value={rateBatch.fromDate}
                        onChange={(e) => setRateBatch((b) => ({ ...b, fromDate: e.target.value }))}
                      />
                    </Field>
                    <Field label="To">
                      <Input
                        type="date"
                        value={rateBatch.toDate}
                        onChange={(e) => setRateBatch((b) => ({ ...b, toDate: e.target.value }))}
                      />
                    </Field>
                    <Field label="Amount (PHP)">
                      <Input
                        type="number"
                        min={0.01}
                        step="0.01"
                        value={rateBatch.amount}
                        onChange={(e) => setRateBatch((b) => ({ ...b, amount: e.target.value }))}
                      />
                    </Field>
                  </div>
                  {editingPlan?.sampleNightlyRate != null && (
                    <p className="text-xs text-muted-foreground">
                      Sample nightly rate: ₱{Number(editingPlan.sampleNightlyRate).toLocaleString()}
                    </p>
                  )}
                </div>
              )}
            </section>
          )}

          {step === 4 && (
            <section className="space-y-4">
              <WizardStepHeader step={4} />
              <div className="mx-auto max-w-md">
                <RoomCatalogCard
                  {...previewCatalog}
                  showFrom
                  bookLabel="Guest view"
                  onBook={() => {}}
                />
              </div>
            </section>
          )}
        </div>
      </div>
    </StaffModal>
  )
}
