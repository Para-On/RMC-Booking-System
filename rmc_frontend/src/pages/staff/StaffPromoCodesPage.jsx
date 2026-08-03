import { useEffect, useMemo, useState } from 'react'
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
import { Checkbox } from '@/components/ui/checkbox'
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
import { formatMoney } from '@/api'
import {
  createStaffPromoCode,
  deleteStaffPromoCode,
  getManagerConfig,
  listStaffPromoCodes,
  updateStaffPromoCode,
} from '@/staffApi'

const EMPTY_FORM = {
  name: '',
  description: '',
  promoType: 'SPECIAL_RATE',
  offerCode: '',
  organizationCode: '',
  discountType: 'PERCENT',
  discountValue: '',
  startsOn: '',
  endsOn: '',
  maxUses: '100',
  active: true,
  ratePlanIds: [],
}

function todayIso() {
  const d = new Date()
  const offset = d.getTimezoneOffset()
  const local = new Date(d.getTime() - offset * 60_000)
  return local.toISOString().slice(0, 10)
}

function needsOrg(type) {
  return type === 'CORPORATE' || type === 'AGENCY'
}

function formatDiscount(row) {
  if (row.discountType === 'PERCENT') {
    return `${Number(row.discountValue)}% off`
  }
  return `${formatMoney(row.discountValue, 'PHP')} off`
}

function typeLabel(type) {
  if (type === 'CORPORATE') return 'Corporate'
  if (type === 'AGENCY') return 'Agency'
  return 'Special rate'
}

function PromoCodeFormFields({ form, setForm, ratePlans }) {
  function togglePlan(id, checked) {
    setForm((prev) => {
      const next = new Set(prev.ratePlanIds.map(String))
      if (checked) next.add(String(id))
      else next.delete(String(id))
      return { ...prev, ratePlanIds: [...next].map(Number) }
    })
  }

  return (
    <div className="grid gap-4">
      <div className="space-y-2">
        <Label htmlFor="pc-name">Name</Label>
        <Input
          id="pc-name"
          value={form.name}
          onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="pc-desc">Description (optional)</Label>
        <Textarea
          id="pc-desc"
          value={form.description}
          onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
          rows={2}
        />
      </div>
      <div className="space-y-2">
        <Label>Promo type</Label>
        <Select
          value={form.promoType}
          onValueChange={(promoType) =>
            setForm((p) => ({
              ...p,
              promoType,
              organizationCode: needsOrg(promoType) ? p.organizationCode : '',
            }))
          }
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="SPECIAL_RATE">Promotion / special rate</SelectItem>
            <SelectItem value="CORPORATE">Corporate rate</SelectItem>
            <SelectItem value="AGENCY">Agency rate</SelectItem>
          </SelectContent>
        </Select>
      </div>
      {needsOrg(form.promoType) ? (
        <div className="space-y-2">
          <Label htmlFor="pc-org">
            {form.promoType === 'AGENCY' ? 'Agency code' : 'Corporate code'}
          </Label>
          <Input
            id="pc-org"
            value={form.organizationCode}
            onChange={(e) => setForm((p) => ({ ...p, organizationCode: e.target.value }))}
            className="uppercase"
          />
        </div>
      ) : null}
      <div className="space-y-2">
        <Label htmlFor="pc-offer">Offer code</Label>
        <Input
          id="pc-offer"
          value={form.offerCode}
          onChange={(e) => setForm((p) => ({ ...p, offerCode: e.target.value }))}
          className="uppercase"
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label>Discount type</Label>
          <Select
            value={form.discountType}
            onValueChange={(discountType) => setForm((p) => ({ ...p, discountType }))}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PERCENT">Percent</SelectItem>
              <SelectItem value="FIXED">Fixed PHP</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="pc-value">
            {form.discountType === 'PERCENT' ? 'Percent' : 'Amount (PHP)'}
          </Label>
          <Input
            id="pc-value"
            type="number"
            min="0.01"
            step="0.01"
            value={form.discountValue}
            onChange={(e) => setForm((p) => ({ ...p, discountValue: e.target.value }))}
          />
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-2">
          <Label htmlFor="pc-start">Starts</Label>
          <Input
            id="pc-start"
            type="date"
            value={form.startsOn}
            onChange={(e) => setForm((p) => ({ ...p, startsOn: e.target.value }))}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="pc-end">Ends</Label>
          <Input
            id="pc-end"
            type="date"
            value={form.endsOn}
            onChange={(e) => setForm((p) => ({ ...p, endsOn: e.target.value }))}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="pc-max">Max uses</Label>
          <Input
            id="pc-max"
            type="number"
            min="1"
            value={form.maxUses}
            onChange={(e) => setForm((p) => ({ ...p, maxUses: e.target.value }))}
          />
        </div>
      </div>
      <div className="flex items-center justify-between rounded-lg border px-3 py-2">
        <Label htmlFor="pc-active">Active</Label>
        <Switch
          id="pc-active"
          checked={form.active}
          onCheckedChange={(active) => setForm((p) => ({ ...p, active }))}
        />
      </div>
      <div className="space-y-2">
        <Label>Rate plans</Label>
        <div className="max-h-48 space-y-2 overflow-y-auto rounded-lg border p-3">
          {ratePlans.length === 0 ? (
            <p className="text-sm text-muted-foreground">No rate plans available.</p>
          ) : (
            ratePlans.map((plan) => (
              <label key={plan.id} className="flex items-center gap-2 text-sm">
                <Checkbox
                  checked={form.ratePlanIds.map(String).includes(String(plan.id))}
                  onCheckedChange={(checked) => togglePlan(plan.id, Boolean(checked))}
                />
                <span>
                  {plan.name}
                  {plan.roomTypeName ? (
                    <span className="text-muted-foreground"> · {plan.roomTypeName}</span>
                  ) : null}
                </span>
              </label>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

export default function StaffPromoCodesPage() {
  const [rows, setRows] = useState([])
  const [ratePlans, setRatePlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)

  const title = useMemo(() => (editingId ? 'Edit promo code' : 'Create promo code'), [editingId])

  useEffect(() => {
    loadAll()
  }, [])

  async function loadAll() {
    setLoading(true)
    setError('')
    try {
      const [codes, config] = await Promise.all([
        listStaffPromoCodes(),
        getManagerConfig().catch(() => ({ ratePlans: [] })),
      ])
      setRows(codes || [])
      setRatePlans(config.ratePlans || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    const start = todayIso()
    setEditingId(null)
    setForm({ ...EMPTY_FORM, startsOn: start, endsOn: start, ratePlanIds: [] })
    setModalOpen(true)
    setMessage('')
  }

  function openEdit(row) {
    setEditingId(row.id)
    setForm({
      name: row.name || '',
      description: row.description || '',
      promoType: row.promoType || 'SPECIAL_RATE',
      offerCode: row.offerCode || '',
      organizationCode: row.organizationCode || '',
      discountType: row.discountType || 'PERCENT',
      discountValue: row.discountValue != null ? String(row.discountValue) : '',
      startsOn: row.startsOn || '',
      endsOn: row.endsOn || '',
      maxUses: row.maxUses != null ? String(row.maxUses) : '100',
      active: row.active !== false,
      ratePlanIds: row.ratePlanIds || [],
    })
    setModalOpen(true)
    setMessage('')
  }

  async function handleSave() {
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || null,
        promoType: form.promoType,
        offerCode: form.offerCode.trim(),
        organizationCode: needsOrg(form.promoType) ? form.organizationCode.trim() : null,
        discountType: form.discountType,
        discountValue: Number(form.discountValue),
        startsOn: form.startsOn,
        endsOn: form.endsOn,
        maxUses: Number(form.maxUses),
        active: form.active,
        ratePlanIds: form.ratePlanIds,
      }
      if (!payload.name) throw new Error('Name is required')
      if (!payload.offerCode) throw new Error('Offer code is required')
      if (needsOrg(form.promoType) && !payload.organizationCode) {
        throw new Error(
          form.promoType === 'AGENCY'
            ? 'Agency code and offer code are both required'
            : 'Corporate code and offer code are both required'
        )
      }
      if (!payload.discountValue || payload.discountValue <= 0) {
        throw new Error('Enter a valid discount value')
      }
      if (!payload.maxUses || payload.maxUses < 1) throw new Error('Max uses must be at least 1')
      if (!payload.startsOn || !payload.endsOn) throw new Error('Start and end dates are required')
      if (!payload.ratePlanIds.length) throw new Error('Select at least one rate plan')

      if (editingId) {
        await updateStaffPromoCode(editingId, payload)
        setMessage('Promo code updated.')
      } else {
        await createStaffPromoCode(payload)
        setMessage('Promo code created.')
      }
      setModalOpen(false)
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(row) {
    if (!window.confirm(`Delete promo code “${row.name}”?`)) return
    setError('')
    setMessage('')
    try {
      await deleteStaffPromoCode(row.id)
      setMessage('Promo code deleted.')
      await loadAll()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <StaffPage
      title="Promo codes"
      description="Access / negotiated rates (special, corporate, agency). Separate from automatic Promos & discounts. Guests enter codes on the home search bar."
      actions={
        <Button type="button" onClick={openCreate}>
          <Plus className="size-4" />
          Add promo code
        </Button>
      }
    >
      {error && <StaffAlert>{error}</StaffAlert>}
      {message && <StaffAlert variant="success">{message}</StaffAlert>}

      <StaffTablePanel>
        <StaffTableWrap>
          <StaffTable>
            <StaffTableHeader>
              <StaffTableRow>
                <StaffTableHead>Code</StaffTableHead>
                <StaffTableHead>Type</StaffTableHead>
                <StaffTableHead>Discount</StaffTableHead>
                <StaffTableHead>Uses</StaffTableHead>
                <StaffTableHead>Rate plans</StaffTableHead>
                <StaffTableHead>Status</StaffTableHead>
                <StaffTableActionsHead />
              </StaffTableRow>
            </StaffTableHeader>
            <StaffTableBody>
              {loading ? (
                <StaffTableRow>
                  <StaffTableCell colSpan={7}>Loading…</StaffTableCell>
                </StaffTableRow>
              ) : rows.length === 0 ? (
                <StaffTableRow>
                  <StaffTableCell colSpan={7}>No promo codes yet.</StaffTableCell>
                </StaffTableRow>
              ) : (
                rows.map((row) => (
                  <StaffTableRow key={row.id}>
                    <StaffTableCell>
                      <div className="font-medium">{row.name}</div>
                      <div className="text-xs text-muted-foreground">
                        {row.organizationCode ? `${row.organizationCode} / ` : ''}
                        {row.offerCode}
                      </div>
                    </StaffTableCell>
                    <StaffTableCell>{typeLabel(row.promoType)}</StaffTableCell>
                    <StaffTableCell>{formatDiscount(row)}</StaffTableCell>
                    <StaffTableCell>
                      {row.usedCount} / {row.maxUses}
                    </StaffTableCell>
                    <StaffTableCell>
                      <div className="max-w-[14rem] text-sm text-muted-foreground">
                        {(row.ratePlanNames || []).join(', ') || '—'}
                      </div>
                    </StaffTableCell>
                    <StaffTableCell>
                      {row.currentlyApplicable ? (
                        <Badge>Live</Badge>
                      ) : row.active ? (
                        <Badge variant="secondary">Scheduled</Badge>
                      ) : (
                        <Badge variant="outline">Inactive</Badge>
                      )}
                    </StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions>
                        <StaffTableAction icon={Pencil} onClick={() => openEdit(row)}>
                          Edit
                        </StaffTableAction>
                        <StaffTableActionSeparator />
                        <StaffTableAction
                          icon={Trash2}
                          variant="destructive"
                          onClick={() => handleDelete(row)}
                        >
                          Delete
                        </StaffTableAction>
                      </StaffTableRowActions>
                    </StaffTableActionsCell>
                  </StaffTableRow>
                ))
              )}
            </StaffTableBody>
          </StaffTable>
        </StaffTableWrap>
      </StaffTablePanel>

      <StaffModal
        open={modalOpen}
        onOpenChange={setModalOpen}
        title={title}
        description="Discounts apply to the tax-inclusive stay total for selected rate plans. Corporate and agency require both organization and offer codes."
        size="lg"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving…' : 'Save promo code'}
            </Button>
          </>
        }
      >
        <PromoCodeFormFields form={form} setForm={setForm} ratePlans={ratePlans} />
      </StaffModal>
    </StaffPage>
  )
}
