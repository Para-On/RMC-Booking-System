import { useEffect, useMemo, useRef, useState } from 'react'
import { ArrowUpDown, Eye, Pencil, Plus, Trash2 } from 'lucide-react'
import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import RoomTypeFormWizard from '@/components/staff/RoomTypeFormWizard'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import RoomOptionSelect from '@/components/staff/RoomOptionSelect'
import { StaffModal } from '@/components/staff/StaffModal'
import StaffTablePagination, { STAFF_PAGE_SIZE_OPTIONS } from '@/components/staff/StaffTablePagination'
import { catalogFromStaffRoom, validateWizardStep, WIZARD_STEPS } from '@/lib/roomCatalog'
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
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
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
import {
  StaffPageTabContent,
  StaffPageTabList,
  StaffPageTabs,
  StaffPageTabTrigger,
} from '@/components/staff/StaffPageTabs'
import {
  createRoomNumber,
  createRoomType,
  deleteRoomNumber,
  getRoomConfigOptions,
  listRoomNumbers,
  listRoomTypes,
  updateRoomNumber,
  updateRoomTypeCatalog,
  updateRoomTypeConfig,
  uploadRoomImage,
} from '@/staffApi'
import { cn } from '@/lib/utils'

const EMPTY_FORM = {
  name: '',
  description: '',
  squareMeters: '',
  maxAdults: '2',
  maxChildren: '1',
  totalCapacity: '1',
  baseNightlyRate: '',
  ratePlanName: 'Standard Flexible',
  refundable: false,
  freeCancellation: true,
  active: true,
  roomCategoryId: '',
  roomViewId: '',
  bedTypeId: '',
}

const PAGE_SIZE_OPTIONS = STAFF_PAGE_SIZE_OPTIONS

function compareText(a, b) {
  return String(a ?? '').localeCompare(String(b ?? ''), undefined, {
    numeric: true,
    sensitivity: 'base',
  })
}

function filterRoomNumbers(list, search) {
  const q = search.trim().toLowerCase()
  if (!q) return list
  return list.filter((unit) =>
    [unit.roomNumber, unit.floorLabel, unit.roomTypeName, unit.statusLabel]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(q))
  )
}

function sortRoomNumbersByNumber(list, ascending) {
  const sorted = [...list]
  sorted.sort((a, b) => {
    const result = compareText(a.roomNumber, b.roomNumber)
    return ascending ? result : -result
  })
  return sorted
}

function filterRoomCatalog(list, search) {
  const q = search.trim().toLowerCase()
  const filtered = !q
    ? list
    : list.filter((room) =>
        [
          room.name,
          room.description,
          room.roomCategoryLabel,
          room.roomViewLabel,
          room.bedTypeLabel,
          ...(room.amenities || []),
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(q))
      )
  return [...filtered].sort((a, b) => compareText(a.name, b.name))
}

function paginateList(list, page, pageSize) {
  const totalPages = Math.max(1, Math.ceil(list.length / pageSize))
  const safePage = Math.min(Math.max(page, 1), totalPages)
  const start = (safePage - 1) * pageSize
  return {
    items: list.slice(start, start + pageSize),
    totalPages,
    safePage,
    total: list.length,
    rangeStart: list.length === 0 ? 0 : start + 1,
    rangeEnd: Math.min(safePage * pageSize, list.length),
  }
}

function TablePager({ page, pageSize, total, rangeStart, rangeEnd, totalPages, onPageChange, onPageSizeChange }) {
  return (
    <StaffTablePagination
      page={Math.max(0, page - 1)}
      pageSize={pageSize}
      totalElements={total}
      totalPages={totalPages}
      onPageChange={(nextPage) => onPageChange(nextPage + 1)}
      onPageSizeChange={onPageSizeChange}
      pageSizeOptions={PAGE_SIZE_OPTIONS}
    />
  )
}

function TabToolbar({ search, onSearchChange, searchPlaceholder }) {
  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
      <Input
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        placeholder={searchPlaceholder}
        className="h-8 max-w-xs text-sm"
        aria-label={searchPlaceholder}
      />
    </div>
  )
}

function NumberColumnHead({ ascending, onToggle }) {
  return (
    <StaffTableHead className="whitespace-nowrap">
      <button
        type="button"
        className="inline-flex items-center gap-1 font-semibold text-foreground/80 transition-colors hover:text-foreground"
        onClick={onToggle}
        aria-label={`Sort by room number ${ascending ? 'descending' : 'ascending'}`}
      >
        Number
        <ArrowUpDown
          className={cn(
            'h-3.5 w-3.5 text-muted-foreground transition-transform',
            !ascending && 'rotate-180',
            'hover:text-foreground'
          )}
        />
      </button>
    </StaffTableHead>
  )
}

function RoomTypeAvatar({ name, imageUrl }) {
  const initial = name?.trim()?.[0]?.toUpperCase() || 'R'
  return (
    <Avatar className="size-9 shrink-0">
      {imageUrl ? <AvatarImage src={imageUrl} alt="" /> : null}
      <AvatarFallback className="bg-muted text-xs font-medium">{initial}</AvatarFallback>
    </Avatar>
  )
}

export default function StaffRoomsCatalogPage() {
  const [rooms, setRooms] = useState([])
  const [roomNumbers, setRoomNumbers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [numberOpen, setNumberOpen] = useState(false)
  const [editUnitOpen, setEditUnitOpen] = useState(false)
  const [editingUnit, setEditingUnit] = useState(null)
  const [editUnitForm, setEditUnitForm] = useState({ roomNumber: '', floorLabel: '', statusOptionId: '' })
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [selectedUnitIds, setSelectedUnitIds] = useState([])
  const [amenities, setAmenities] = useState([])
  const [amenityInput, setAmenityInput] = useState('')
  const [imageUrls, setImageUrls] = useState([])
  const [newRoomNumber, setNewRoomNumber] = useState({ roomNumber: '', floorLabel: '' })
  const [roomConfig, setRoomConfig] = useState({ categories: [], views: [], bedTypes: [], statuses: [] })
  const [formMode, setFormMode] = useState('create')
  const [editingRoomId, setEditingRoomId] = useState(null)
  const [wizardStep, setWizardStep] = useState(0)
  const [stepError, setStepError] = useState('')
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewRoom, setPreviewRoom] = useState(null)
  const fileInputRef = useRef(null)
  const [activeTab, setActiveTab] = useState('numbers')
  const [numbersSearch, setNumbersSearch] = useState('')
  const [numbersSortAsc, setNumbersSortAsc] = useState(true)
  const [numbersPage, setNumbersPage] = useState(1)
  const [numbersPageSize, setNumbersPageSize] = useState(10)
  const [catalogSearch, setCatalogSearch] = useState('')
  const [catalogPage, setCatalogPage] = useState(1)
  const [catalogPageSize, setCatalogPageSize] = useState(10)

  function dayStatusVariant(status) {
    switch (status) {
      case 'AVAILABLE':
        return 'outline'
      case 'RESERVED':
        return 'secondary'
      case 'OCCUPIED':
        return 'default'
      case 'OUT_OF_ORDER':
        return 'destructive'
      default:
        return 'outline'
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  async function loadAll() {
    setLoading(true)
    setError('')
    try {
      const [roomData, numberData, configData] = await Promise.all([
        listRoomTypes(),
        listRoomNumbers(false),
        getRoomConfigOptions(),
      ])
      setRooms(roomData)
      setRoomNumbers(numberData)
      setRoomConfig(configData)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const selectableNumbers = roomNumbers

  const filteredNumbers = useMemo(
    () => sortRoomNumbersByNumber(filterRoomNumbers(roomNumbers, numbersSearch), numbersSortAsc),
    [roomNumbers, numbersSearch, numbersSortAsc]
  )
  const numbersPagination = useMemo(
    () => paginateList(filteredNumbers, numbersPage, numbersPageSize),
    [filteredNumbers, numbersPage, numbersPageSize]
  )

  const filteredCatalog = useMemo(
    () => filterRoomCatalog(rooms, catalogSearch),
    [rooms, catalogSearch]
  )
  const catalogPagination = useMemo(
    () => paginateList(filteredCatalog, catalogPage, catalogPageSize),
    [filteredCatalog, catalogPage, catalogPageSize]
  )

  useEffect(() => {
    if (numbersPage !== numbersPagination.safePage) {
      setNumbersPage(numbersPagination.safePage)
    }
  }, [numbersPage, numbersPagination.safePage])

  useEffect(() => {
    if (catalogPage !== catalogPagination.safePage) {
      setCatalogPage(catalogPagination.safePage)
    }
  }, [catalogPage, catalogPagination.safePage])

  function openCreateRoomType() {
    setFormMode('create')
    setEditingRoomId(null)
    setWizardStep(0)
    setStepError('')
    setMessage('')
    setError('')
    setCreateOpen(true)
  }

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function toggleUnit(id, checked) {
    setSelectedUnitIds((prev) =>
      checked ? [...prev, id] : prev.filter((unitId) => unitId !== id)
    )
  }

  function addAmenity() {
    const value = amenityInput.trim()
    if (!value || amenities.includes(value)) return
    setAmenities((prev) => [...prev, value])
    setAmenityInput('')
  }

  function removeAmenity(value) {
    setAmenities((prev) => prev.filter((item) => item !== value))
  }

  async function handleImageUpload(event) {
    const files = Array.from(event.target.files || [])
    if (files.length === 0) return
    setUploading(true)
    setError('')
    try {
      const uploaded = []
      for (const file of files) {
        const result = await uploadRoomImage(file)
        uploaded.push(result.imageUrl)
      }
      setImageUrls((prev) => [...prev, ...uploaded])
    } catch (err) {
      setError(err.message)
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  function removeImage(url) {
    setImageUrls((prev) => prev.filter((item) => item !== url))
  }

  function resetCreateForm() {
    setForm(EMPTY_FORM)
    setSelectedUnitIds([])
    setAmenities([])
    setAmenityInput('')
    setImageUrls([])
    setFormMode('create')
    setEditingRoomId(null)
    setWizardStep(0)
    setStepError('')
    setMessage('')
    setError('')
  }

  function openPreviewRoom(room) {
    setPreviewRoom(room)
    setPreviewOpen(true)
  }

  function goNextWizardStep() {
    const err = validateWizardStep(wizardStep, { form, selectedUnitIds, amenities })
    if (err) {
      setStepError(err)
      return
    }
    setStepError('')
    setWizardStep((s) => Math.min(s + 1, WIZARD_STEPS.length - 1))
  }

  function goBackWizardStep() {
    setStepError('')
    setWizardStep((s) => Math.max(s - 1, 0))
  }

  function openEditRoom(room) {
    setMessage('')
    setError('')
    setFormMode('edit')
    setEditingRoomId(room.id)
    setForm({
      name: room.name || '',
      description: room.description || '',
      squareMeters: room.squareMeters != null ? String(room.squareMeters) : '',
      maxAdults: String(room.maxAdults ?? 2),
      maxChildren: String(room.maxChildren ?? 1),
      totalCapacity: String(room.totalCapacity ?? room.unitCount ?? 1),
      baseNightlyRate: room.baseNightlyRate != null ? String(room.baseNightlyRate) : '',
      ratePlanName: room.ratePlanName || 'Standard Flexible',
      refundable: Boolean(room.refundable),
      freeCancellation: room.freeCancellation !== false,
      active: room.active !== false,
      roomCategoryId: room.roomCategoryId ? String(room.roomCategoryId) : '',
      roomViewId: room.roomViewId ? String(room.roomViewId) : '',
      bedTypeId: room.bedTypeId ? String(room.bedTypeId) : '',
    })
    setAmenities(room.amenities || [])
    setImageUrls(room.imageUrls || [])
    setSelectedUnitIds((room.assignedRoomNumbers || []).map((unit) => unit.id))
    setWizardStep(0)
    setStepError('')
    setCreateOpen(true)
  }

  function openEditUnit(unit) {
    setEditingUnit(unit)
    setEditUnitForm({
      roomNumber: unit.roomNumber || '',
      floorLabel: unit.floorLabel || '',
      statusOptionId: unit.statusOptionId ? String(unit.statusOptionId) : '',
    })
    setEditUnitOpen(true)
  }

  function closeEditUnit() {
    setEditUnitOpen(false)
    setEditingUnit(null)
    setEditUnitForm({ roomNumber: '', floorLabel: '', statusOptionId: '' })
  }

  async function handleUpdateUnit(e) {
    e.preventDefault()
    if (!editingUnit) return
    if (!editUnitForm.statusOptionId) {
      setError('Select a room status.')
      return
    }
    setSaving(true)
    setError('')
    setMessage('')
    try {
      await updateRoomNumber(editingUnit.id, {
        roomNumber: editUnitForm.roomNumber.trim(),
        floorLabel: editUnitForm.floorLabel.trim() || null,
        statusOptionId: Number(editUnitForm.statusOptionId),
      })
      setMessage(`Room ${editUnitForm.roomNumber.trim()} updated.`)
      closeEditUnit()
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDeleteUnit(unit) {
    const assignmentNote = unit.roomTypeName
      ? ` It is currently assigned to ${unit.roomTypeName}.`
      : ''
    const confirmed = window.confirm(
      `Delete room ${unit.roomNumber}? This cannot be undone.${assignmentNote}`
    )
    if (!confirmed) return
    setError('')
    setMessage('')
    try {
      await deleteRoomNumber(unit.id)
      setMessage(`Room ${unit.roomNumber} deleted.`)
      await loadAll()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCreateRoomNumber(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const created = await createRoomNumber(newRoomNumber.roomNumber.trim(), newRoomNumber.floorLabel.trim())
      setNewRoomNumber({ roomNumber: '', floorLabel: '' })
      setNumberOpen(false)
      await loadAll()
      if (createOpen) {
        setSelectedUnitIds((prev) => [...new Set([...prev, created.id])])
      } else {
        setMessage(`Room ${created.roomNumber} added.`)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleCreateRoom(e) {
    e.preventDefault()
    // Enter in a field submits the form — advance steps instead of saving early.
    if (wizardStep < WIZARD_STEPS.length - 1) {
      goNextWizardStep()
      return
    }
    for (let step = 0; step < WIZARD_STEPS.length - 1; step++) {
      const err = validateWizardStep(step, { form, selectedUnitIds, amenities })
      if (err) {
        setStepError(err)
        setWizardStep(step)
        return
      }
    }
    setStepError('')
    setSaving(true)
    setError('')
    setMessage('')
    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      squareMeters: form.squareMeters ? Number(form.squareMeters) : null,
      maxAdults: Number(form.maxAdults),
      maxChildren: Number(form.maxChildren),
      totalCapacity: Number(form.totalCapacity),
      active: form.active,
      ratePlanName: form.ratePlanName.trim(),
      refundable: form.refundable,
      freeCancellation: form.freeCancellation,
      baseNightlyRate: Number(form.baseNightlyRate),
      roomUnitIds: selectedUnitIds,
      amenities,
      imageUrls,
      roomCategoryId: Number(form.roomCategoryId),
      roomViewId: Number(form.roomViewId),
      bedTypeId: Number(form.bedTypeId),
    }
    try {
      const successMessage =
        formMode === 'edit' && editingRoomId
          ? 'Room type updated.'
          : 'Room type created and linked to selected room numbers.'
      if (formMode === 'edit' && editingRoomId) {
        await updateRoomTypeCatalog(editingRoomId, payload)
      } else {
        await createRoomType(payload)
      }
      resetCreateForm()
      setCreateOpen(false)
      setMessage(successMessage)
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive(room) {
    setError('')
    try {
      await updateRoomTypeConfig(room.id, { active: !room.active })
      await loadAll()
      setMessage(`Room ${room.active ? 'hidden from' : 'visible on'} guest search.`)
    } catch (err) {
      setError(err.message)
    }
  }

  const modals = (
    <>
      <StaffModal
        open={numberOpen}
        onOpenChange={(open) => {
          setNumberOpen(open)
          if (!open) {
            setNewRoomNumber({ roomNumber: '', floorLabel: '' })
            setError('')
          }
        }}
        title="Add room number"
        description="Create a physical room number before defining its room type and guest-facing details."
        size="md"
        footer={
          <>
            <Button type="button" variant="outline" size="sm" onClick={() => setNumberOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="add-room-number-form" size="sm" disabled={saving}>
              {saving ? 'Saving…' : 'Add room number'}
            </Button>
          </>
        }
      >
        <form id="add-room-number-form" className="grid gap-5 sm:grid-cols-2" onSubmit={handleCreateRoomNumber}>
          <div className="grid gap-2 sm:col-span-1">
            <Label htmlFor="room-number">Room number</Label>
            <Input
              id="room-number"
              value={newRoomNumber.roomNumber}
              onChange={(e) => setNewRoomNumber((prev) => ({ ...prev, roomNumber: e.target.value }))}
              placeholder="301"
              required
            />
          </div>
          <div className="grid gap-2 sm:col-span-1">
            <Label htmlFor="floor-label">Floor (optional)</Label>
            <Input
              id="floor-label"
              value={newRoomNumber.floorLabel}
              onChange={(e) => setNewRoomNumber((prev) => ({ ...prev, floorLabel: e.target.value }))}
              placeholder="3F"
            />
          </div>
        </form>
      </StaffModal>

      <StaffModal
        open={createOpen}
        onOpenChange={(open) => {
          setCreateOpen(open)
          if (!open) resetCreateForm()
        }}
        title={formMode === 'edit' ? 'Edit room type' : 'Create room type'}
        description={
          formMode === 'edit'
            ? `Step ${wizardStep + 1} of ${WIZARD_STEPS.length} — update specs and preview the guest room card.`
            : `Step ${wizardStep + 1} of ${WIZARD_STEPS.length} — define the room catalog entry guests will see when booking.`
        }
        size="wizard"
        bodyClassName="flex min-h-0 flex-1 flex-col overflow-hidden py-3 sm:py-4"
        footer={
          <>
            <Button type="button" variant="outline" size="sm" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            {wizardStep > 0 && (
              <Button type="button" variant="outline" size="sm" onClick={goBackWizardStep}>
                Back
              </Button>
            )}
            {wizardStep < WIZARD_STEPS.length - 1 ? (
              <Button
                type="button"
                size="sm"
                onClick={goNextWizardStep}
                disabled={wizardStep === 0 && selectedUnitIds.length === 0}
              >
                Next
              </Button>
            ) : (
              <Button
                type="submit"
                form="create-room-type-form"
                size="sm"
                disabled={saving || selectedUnitIds.length === 0}
              >
                {saving ? 'Saving…' : formMode === 'edit' ? 'Save changes' : 'Create room type'}
              </Button>
            )}
          </>
        }
      >
        <form id="create-room-type-form" className="flex h-full min-h-0 flex-col" onSubmit={handleCreateRoom}>
          <RoomTypeFormWizard
            wizardStep={wizardStep}
            form={form}
            updateField={updateField}
            amenities={amenities}
            amenityInput={amenityInput}
            setAmenityInput={setAmenityInput}
            addAmenity={addAmenity}
            removeAmenity={removeAmenity}
            imageUrls={imageUrls}
            removeImage={removeImage}
            uploading={uploading}
            fileInputRef={fileInputRef}
            handleImageUpload={handleImageUpload}
            selectedUnitIds={selectedUnitIds}
            toggleUnit={toggleUnit}
            selectableNumbers={selectableNumbers}
            editingRoomId={editingRoomId}
            roomConfig={roomConfig}
            stepError={stepError}
          />
          {error && createOpen && !stepError && <StaffAlert className="mt-4">{error}</StaffAlert>}
        </form>
      </StaffModal>

      <StaffModal
        open={previewOpen}
        onOpenChange={(open) => {
          setPreviewOpen(open)
          if (!open) setPreviewRoom(null)
        }}
        title="Guest room card preview"
        description="This is how the room appears on the public booking site."
        size="lg"
        footer={
          <Button type="button" variant="outline" size="sm" onClick={() => setPreviewOpen(false)}>
            Close
          </Button>
        }
      >
        {previewRoom && <RoomCatalogCard {...catalogFromStaffRoom(previewRoom)} />}
      </StaffModal>
    </>
  )

  return (
    <StaffPage
      title="Create room"
      description="Add room numbers, define room types, and manage the guest-facing room catalog."
      actions={
        activeTab === 'numbers' ? (
          <Button type="button" size="sm" variant="outline" onClick={() => setNumberOpen(true)}>
            <Plus className="h-3.5 w-3.5" />
            Add room number
          </Button>
        ) : (
          <Button type="button" size="sm" onClick={openCreateRoomType}>
            <Plus className="h-3.5 w-3.5" />
            Create room type
          </Button>
        )
      }
    >
      <StaffAlert variant="success">{!createOpen && !numberOpen && !editUnitOpen ? message : ''}</StaffAlert>
      <StaffAlert>{error && !createOpen && !numberOpen && !editUnitOpen ? error : ''}</StaffAlert>

      <StaffModal
        open={editUnitOpen}
        onOpenChange={(open) => {
          if (!open) closeEditUnit()
          else setEditUnitOpen(true)
        }}
        title="Edit room number"
        description="Update room number details and maintenance status."
        size="md"
        footer={
          <>
            <Button type="button" variant="outline" onClick={closeEditUnit}>
              Cancel
            </Button>
            <Button type="submit" form="edit-room-number-form" disabled={saving}>
              {saving ? 'Saving…' : 'Save changes'}
            </Button>
          </>
        }
      >
        <form id="edit-room-number-form" className="space-y-5" onSubmit={handleUpdateUnit}>
          {editingUnit && (
            <div className="flex flex-wrap items-center gap-2 text-sm">
              <Badge variant={dayStatusVariant(editingUnit.dayStatus)}>{editingUnit.statusLabel}</Badge>
              {editingUnit.roomTypeName ? (
                <Badge variant="secondary">{editingUnit.roomTypeName}</Badge>
              ) : (
                <Badge variant="outline">Unassigned</Badge>
              )}
            </div>
          )}
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="grid gap-2 sm:col-span-1">
              <Label htmlFor="edit-room-number">Room number</Label>
              <Input
                id="edit-room-number"
                value={editUnitForm.roomNumber}
                onChange={(e) => setEditUnitForm((prev) => ({ ...prev, roomNumber: e.target.value }))}
                required
              />
            </div>
            <div className="grid gap-2 sm:col-span-1">
              <Label htmlFor="edit-floor-label">Floor (optional)</Label>
              <Input
                id="edit-floor-label"
                value={editUnitForm.floorLabel}
                onChange={(e) => setEditUnitForm((prev) => ({ ...prev, floorLabel: e.target.value }))}
              />
            </div>
          </div>
          <RoomOptionSelect
            label="Status"
            options={roomConfig.statuses || []}
            value={editUnitForm.statusOptionId}
            onChange={(value) => setEditUnitForm((prev) => ({ ...prev, statusOptionId: value }))}
          />
          {error && editUnitOpen && <StaffAlert>{error}</StaffAlert>}
        </form>
      </StaffModal>

      {modals}

      <StaffPageTabs value={activeTab} onValueChange={setActiveTab}>
        <StaffPageTabList>
          <StaffPageTabTrigger value="numbers">Room numbers</StaffPageTabTrigger>
          <StaffPageTabTrigger value="catalog">Room catalog</StaffPageTabTrigger>
        </StaffPageTabList>

        <StaffPageTabContent value="numbers" className="space-y-3">
          <TabToolbar
            search={numbersSearch}
            onSearchChange={(value) => {
              setNumbersSearch(value)
              setNumbersPage(1)
            }}
            searchPlaceholder="Search room, floor, type…"
          />

          {loading && <p className="text-sm text-muted-foreground">Loading room numbers…</p>}
          {!loading && roomNumbers.length === 0 && (
            <p className="text-sm text-muted-foreground">No room numbers yet. Add your first room number.</p>
          )}
          {!loading && roomNumbers.length > 0 && filteredNumbers.length === 0 && (
            <p className="text-sm text-muted-foreground">No room numbers match your search.</p>
          )}
          {!loading && numbersPagination.items.length > 0 && (
            <StaffTablePanel>
              <StaffTableWrap>
                <StaffTable>
                  <StaffTableHeader>
                    <StaffTableRow>
                      <NumberColumnHead
                        ascending={numbersSortAsc}
                        onToggle={() => {
                          setNumbersSortAsc((prev) => !prev)
                          setNumbersPage(1)
                        }}
                      />
                      <StaffTableHead className="hidden sm:table-cell">Floor</StaffTableHead>
                      <StaffTableHead>Assignment</StaffTableHead>
                      <StaffTableHead className="hidden md:table-cell">Status</StaffTableHead>
                      <StaffTableActionsHead />
                    </StaffTableRow>
                  </StaffTableHeader>
                  <StaffTableBody>
                    {numbersPagination.items.map((unit) => (
                      <StaffTableRow key={unit.id}>
                        <StaffTableCell className="font-medium">{unit.roomNumber}</StaffTableCell>
                        <StaffTableCell className="hidden sm:table-cell">{unit.floorLabel || '—'}</StaffTableCell>
                        <StaffTableCell>
                          {unit.roomTypeName ? (
                            <Badge variant="secondary" className="text-xs">
                              {unit.roomTypeName}
                            </Badge>
                          ) : (
                            <Badge variant="outline" className="text-xs">
                              Unassigned
                            </Badge>
                          )}
                        </StaffTableCell>
                        <StaffTableCell className="hidden md:table-cell">
                          <Badge variant={dayStatusVariant(unit.dayStatus)} className="text-xs">
                            {unit.statusLabel}
                          </Badge>
                        </StaffTableCell>
                        <StaffTableActionsCell>
                          <StaffTableRowActions label={`Actions for room ${unit.roomNumber}`}>
                            <StaffTableAction icon={Pencil} onClick={() => openEditUnit(unit)}>
                              Edit
                            </StaffTableAction>
                            <StaffTableActionSeparator />
                            <StaffTableAction
                              icon={Trash2}
                              variant="destructive"
                              onClick={() => handleDeleteUnit(unit)}
                            >
                              Delete
                            </StaffTableAction>
                          </StaffTableRowActions>
                        </StaffTableActionsCell>
                      </StaffTableRow>
                    ))}
                  </StaffTableBody>
                </StaffTable>
              </StaffTableWrap>
              <TablePager
                page={numbersPagination.safePage}
                pageSize={numbersPageSize}
                total={numbersPagination.total}
                rangeStart={numbersPagination.rangeStart}
                rangeEnd={numbersPagination.rangeEnd}
                totalPages={numbersPagination.totalPages}
                onPageChange={setNumbersPage}
                onPageSizeChange={(size) => {
                  setNumbersPageSize(size)
                  setNumbersPage(1)
                }}
              />
            </StaffTablePanel>
          )}
        </StaffPageTabContent>

        <StaffPageTabContent value="catalog" className="space-y-3">
          <TabToolbar
            search={catalogSearch}
            onSearchChange={(value) => {
              setCatalogSearch(value)
              setCatalogPage(1)
            }}
            searchPlaceholder="Search name, category, amenity…"
          />

          {loading && <p className="text-sm text-muted-foreground">Loading rooms…</p>}
          {!loading && rooms.length === 0 && (
            <p className="text-sm text-muted-foreground">No room types yet.</p>
          )}
          {!loading && rooms.length > 0 && filteredCatalog.length === 0 && (
            <p className="text-sm text-muted-foreground">No room types match your search.</p>
          )}
          {!loading && catalogPagination.items.length > 0 && (
            <StaffTablePanel>
              <StaffTableWrap>
                <StaffTable>
                  <StaffTableHeader>
                    <StaffTableRow>
                      <StaffTableHead>Room</StaffTableHead>
                      <StaffTableHead className="hidden md:table-cell">Category</StaffTableHead>
                      <StaffTableHead className="hidden lg:table-cell">View / Bed</StaffTableHead>
                      <StaffTableHead>Units</StaffTableHead>
                      <StaffTableHead>Rate</StaffTableHead>
                      <StaffTableHead>Status</StaffTableHead>
                      <StaffTableActionsHead />
                    </StaffTableRow>
                  </StaffTableHeader>
                  <StaffTableBody>
                    {catalogPagination.items.map((room) => (
                      <StaffTableRow key={room.id}>
                        <StaffTableCell>
                          <div className="flex items-center gap-2.5">
                            <RoomTypeAvatar name={room.name} imageUrl={room.imageUrls?.[0]} />
                            <span className="font-medium">{room.name}</span>
                          </div>
                        </StaffTableCell>
                        <StaffTableCell className="hidden md:table-cell">
                          {room.roomCategoryLabel || '—'}
                        </StaffTableCell>
                        <StaffTableCell className="hidden lg:table-cell text-muted-foreground">
                          {[room.roomViewLabel, room.bedTypeLabel].filter(Boolean).join(' · ') || '—'}
                        </StaffTableCell>
                        <StaffTableCell>{room.unitCount}</StaffTableCell>
                        <StaffTableCell>
                          {room.baseNightlyRate != null
                            ? `₱${Number(room.baseNightlyRate).toLocaleString()}`
                            : '—'}
                        </StaffTableCell>
                        <StaffTableCell>
                          <Badge variant={room.active ? 'default' : 'secondary'} className="text-xs">
                            {room.active ? 'Active' : 'Hidden'}
                          </Badge>
                        </StaffTableCell>
                        <StaffTableActionsCell>
                          <StaffTableRowActions label={`Actions for ${room.name}`}>
                            <StaffTableAction icon={Eye} onClick={() => openPreviewRoom(room)}>
                              View
                            </StaffTableAction>
                            <StaffTableAction icon={Pencil} onClick={() => openEditRoom(room)}>
                              Edit
                            </StaffTableAction>
                            <StaffTableAction onClick={() => toggleActive(room)}>
                              {room.active ? 'Hide from guests' : 'Show to guests'}
                            </StaffTableAction>
                          </StaffTableRowActions>
                        </StaffTableActionsCell>
                      </StaffTableRow>
                    ))}
                  </StaffTableBody>
                </StaffTable>
              </StaffTableWrap>
              <TablePager
                page={catalogPagination.safePage}
                pageSize={catalogPageSize}
                total={catalogPagination.total}
                rangeStart={catalogPagination.rangeStart}
                rangeEnd={catalogPagination.rangeEnd}
                totalPages={catalogPagination.totalPages}
                onPageChange={setCatalogPage}
                onPageSizeChange={(size) => {
                  setCatalogPageSize(size)
                  setCatalogPage(1)
                }}
              />
            </StaffTablePanel>
          )}
        </StaffPageTabContent>
      </StaffPageTabs>
    </StaffPage>
  )
}
