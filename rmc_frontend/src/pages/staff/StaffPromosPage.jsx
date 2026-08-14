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
import { useAppFeedback } from '@/context/AppFeedbackProvider'
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
  createStaffPromo,
  deleteStaffPromo,
  listRoomTypes,
  listStaffPromos,
  updateStaffPromo,
} from '@/staffApi'

const EMPTY_FORM = {
  name: '',
  description: '',
  discountType: 'PERCENT',
  discountValue: '',
  startsOn: '',
  endsOn: '',
  active: true,
  roomTypeIds: [],
}

function todayIso() {
  const d = new Date()
  const offset = d.getTimezoneOffset()
  const local = new Date(d.getTime() - offset * 60_000)
  return local.toISOString().slice(0, 10)
}

function formatDiscount(promo) {
  if (promo.discountType === 'PERCENT') {
    return `${Number(promo.discountValue)}% off`
  }
  return `${formatMoney(promo.discountValue, 'PHP')} off`
}

function PromoFormFields({ form, setForm, roomTypes }) {
  function toggleRoomType(id, checked) {
    setForm((prev) => {
      const next = new Set(prev.roomTypeIds.map(String))
      if (checked) next.add(String(id))
      else next.delete(String(id))
      return { ...prev, roomTypeIds: [...next].map(Number) }
    })
  }

  return (
    <div className="grid gap-4">
      <div className="space-y-2">
        <Label htmlFor="promo-name">Name</Label>
        <Input
          id="promo-name"
          value={form.name}
          onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
          placeholder="Weekend escape"
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="promo-description">Description (optional)</Label>
        <Textarea
          id="promo-description"
          value={form.description}
          onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
          rows={2}
          placeholder="Shown to guests on search and checkout when applied"
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label>Discount type</Label>
          <Select
            value={form.discountType}
            onValueChange={(value) => setForm((prev) => ({ ...prev, discountType: value }))}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PERCENT">Percent</SelectItem>
              <SelectItem value="FIXED">Fixed amount (₱)</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="promo-value">
            {form.discountType === 'PERCENT' ? 'Percent off' : 'Amount off (₱)'}
          </Label>
          <Input
            id="promo-value"
            type="number"
            min="0.01"
            step="0.01"
            value={form.discountValue}
            onChange={(e) => setForm((prev) => ({ ...prev, discountValue: e.target.value }))}
            required
          />
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="promo-starts">Starts on</Label>
          <Input
            id="promo-starts"
            type="date"
            value={form.startsOn}
            onChange={(e) => setForm((prev) => ({ ...prev, startsOn: e.target.value }))}
            required
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="promo-ends">Ends on</Label>
          <Input
            id="promo-ends"
            type="date"
            value={form.endsOn}
            onChange={(e) => setForm((prev) => ({ ...prev, endsOn: e.target.value }))}
            required
          />
        </div>
      </div>
      <div className="flex items-center justify-between rounded-lg border px-3 py-2">
        <div>
          <p className="text-sm font-medium">Active</p>
          <p className="text-xs text-muted-foreground">
            Inactive promos never apply, even within their date window.
          </p>
        </div>
        <Switch
          checked={form.active}
          onCheckedChange={(value) => setForm((prev) => ({ ...prev, active: Boolean(value) }))}
        />
      </div>
      <div className="space-y-2">
        <Label>Apply to room types</Label>
        <div className="max-h-48 space-y-2 overflow-y-auto rounded-lg border p-3">
          {roomTypes.length === 0 ? (
            <p className="text-sm text-muted-foreground">No room types available.</p>
          ) : (
            roomTypes.map((room) => {
              const checked = form.roomTypeIds.map(String).includes(String(room.id))
              return (
                <label key={room.id} className="flex cursor-pointer items-center gap-2 text-sm">
                  <Checkbox
                    checked={checked}
                    onCheckedChange={(value) => toggleRoomType(room.id, Boolean(value))}
                  />
                  <span>{room.name}</span>
                </label>
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}

export default function StaffPromosPage() {
  const { confirm } = useAppFeedback()
  const [promos, setPromos] = useState([])
  const [roomTypes, setRoomTypes] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)

  const title = useMemo(() => (editingId ? 'Edit promo' : 'Create promo'), [editingId])

  useEffect(() => {
    loadAll()
  }, [])

  async function loadAll() {
    setLoading(true)
    setError('')
    try {
      const [promoRows, rooms] = await Promise.all([listStaffPromos(), listRoomTypes()])
      setPromos(promoRows || [])
      setRoomTypes(rooms || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    const start = todayIso()
    setEditingId(null)
    setForm({
      ...EMPTY_FORM,
      startsOn: start,
      endsOn: start,
      roomTypeIds: [],
    })
    setModalOpen(true)
    setMessage('')
  }

  function openEdit(promo) {
    setEditingId(promo.id)
    setForm({
      name: promo.name || '',
      description: promo.description || '',
      discountType: promo.discountType || 'PERCENT',
      discountValue: promo.discountValue != null ? String(promo.discountValue) : '',
      startsOn: promo.startsOn || '',
      endsOn: promo.endsOn || '',
      active: promo.active !== false,
      roomTypeIds: promo.roomTypeIds || [],
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
        discountType: form.discountType,
        discountValue: Number(form.discountValue),
        startsOn: form.startsOn,
        endsOn: form.endsOn,
        active: form.active,
        roomTypeIds: form.roomTypeIds,
      }
      if (!payload.name) throw new Error('Name is required')
      if (!payload.discountValue || payload.discountValue <= 0) {
        throw new Error('Enter a valid discount value')
      }
      if (!payload.startsOn || !payload.endsOn) throw new Error('Start and end dates are required')
      if (!payload.roomTypeIds.length) throw new Error('Select at least one room type')

      if (editingId) {
        await updateStaffPromo(editingId, payload)
        setMessage('Promo updated.')
      } else {
        await createStaffPromo(payload)
        setMessage('Promo created.')
      }
      setModalOpen(false)
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(promo) {
    const decision = await confirm({
      title: 'Delete promo?',
      description: `Delete promo “${promo.name}”?`,
      confirmLabel: 'Delete',
      variant: 'destructive',
    })
    if (!decision.confirmed) return
    setError('')
    setMessage('')
    try {
      await deleteStaffPromo(promo.id)
      setMessage('Promo deleted.')
      await loadAll()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <StaffPage
      title="Promos & discounts"
      description="Create time-bound percent or fixed discounts and assign them to catalog room types. Expired or inactive promos stop appearing on guest search and checkout."
      actions={
        <Button type="button" onClick={openCreate}>
          <Plus className="size-4" />
          Add promo
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
                <StaffTableHead>Promo</StaffTableHead>
                <StaffTableHead>Discount</StaffTableHead>
                <StaffTableHead>Window</StaffTableHead>
                <StaffTableHead>Room types</StaffTableHead>
                <StaffTableHead>Status</StaffTableHead>
                <StaffTableActionsHead />
              </StaffTableRow>
            </StaffTableHeader>
            <StaffTableBody>
              {loading ? (
                <StaffTableRow>
                  <StaffTableCell colSpan={6}>Loading…</StaffTableCell>
                </StaffTableRow>
              ) : promos.length === 0 ? (
                <StaffTableRow>
                  <StaffTableCell colSpan={6}>No promos yet. Create one to offer guest discounts.</StaffTableCell>
                </StaffTableRow>
              ) : (
                promos.map((promo) => (
                  <StaffTableRow key={promo.id}>
                    <StaffTableCell>
                      <div className="font-medium">{promo.name}</div>
                      {promo.description ? (
                        <div className="text-xs text-muted-foreground line-clamp-2">
                          {promo.description}
                        </div>
                      ) : null}
                    </StaffTableCell>
                    <StaffTableCell>{formatDiscount(promo)}</StaffTableCell>
                    <StaffTableCell>
                      {promo.startsOn} → {promo.endsOn}
                    </StaffTableCell>
                    <StaffTableCell>
                      <div className="max-w-[14rem] text-sm text-muted-foreground">
                        {(promo.roomTypeNames || []).join(', ') || '—'}
                      </div>
                    </StaffTableCell>
                    <StaffTableCell>
                      {promo.currentlyEffective ? (
                        <Badge>Live</Badge>
                      ) : promo.active ? (
                        <Badge variant="secondary">Scheduled</Badge>
                      ) : (
                        <Badge variant="outline">Inactive</Badge>
                      )}
                    </StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions>
                        <StaffTableAction icon={Pencil} onClick={() => openEdit(promo)}>
                          Edit
                        </StaffTableAction>
                        <StaffTableActionSeparator />
                        <StaffTableAction
                          icon={Trash2}
                          variant="destructive"
                          onClick={() => handleDelete(promo)}
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
        description="Discounts apply to the room tax-inclusive stay total before extras. When multiple promos match a room, the largest savings wins."
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving…' : 'Save promo'}
            </Button>
          </>
        }
      >
        <PromoFormFields form={form} setForm={setForm} roomTypes={roomTypes} />
      </StaffModal>
    </StaffPage>
  )
}
