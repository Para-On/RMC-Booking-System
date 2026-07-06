import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight, ImagePlus, Trash2, X } from 'lucide-react'
import RoomCatalogCard from '@/components/room/RoomCatalogCard'
import RoomOptionSelect from '@/components/staff/RoomOptionSelect'
import RoomTypeWizardStepper from '@/components/staff/RoomTypeWizardStepper'
import { StaffAlert } from '@/components/staff/StaffPageShell'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { catalogFromWizardForm, WIZARD_STEP_CONTENT, WIZARD_STEPS } from '@/lib/roomCatalog'
import { cn } from '@/lib/utils'

function sortByRoomNumber(units) {
  return [...units].sort((a, b) =>
    String(a.roomNumber).localeCompare(String(b.roomNumber), undefined, { numeric: true })
  )
}

function roomAssignmentStyles(unit, editingRoomId) {
  if (!unit.roomTypeId) {
    return {
      card: 'border-dashed border-muted-foreground/35 bg-background',
      label: 'font-medium text-muted-foreground',
    }
  }
  if (unit.roomTypeId === editingRoomId) {
    return {
      card: 'border-primary/60 bg-primary/5',
      label: 'font-medium text-primary',
    }
  }
  return {
    card: 'border-violet-300/80 bg-violet-50/90 dark:border-violet-700/50 dark:bg-violet-950/35',
    label: 'font-medium text-violet-700 dark:text-violet-300',
  }
}

function assignmentLabel(unit, editingRoomId) {
  if (!unit.roomTypeId) return 'Unassigned'
  if (unit.roomTypeId !== editingRoomId) return unit.roomTypeName || 'Other room type'
  return unit.floorLabel ? `Fl. ${unit.floorLabel}` : unit.roomTypeName || 'This room type'
}

function RoomAssignmentLegend() {
  const items = [
    { label: 'Unassigned', swatch: 'border border-dashed border-muted-foreground/40 bg-background' },
    { label: 'This room type', swatch: 'border border-primary/60 bg-primary/10' },
    { label: 'Other room type', swatch: 'border border-violet-300/80 bg-violet-50 dark:border-violet-700/50 dark:bg-violet-950/40' },
  ]
  return (
    <div className="flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-muted-foreground">
      {items.map((item) => (
        <span key={item.label} className="inline-flex items-center gap-2">
          <span className={cn('h-3 w-3 shrink-0 rounded-sm', item.swatch)} aria-hidden />
          {item.label}
        </span>
      ))}
    </div>
  )
}

function WizardStepHeader({ step, legend }) {
  const content = WIZARD_STEP_CONTENT[step]
  if (!content) return null
  return (
    <div className="mt-4 shrink-0 space-y-2">
      <h3 className="text-sm font-medium">{content.title}</h3>
      <p className="text-sm text-muted-foreground">{content.description}</p>
      {legend}
    </div>
  )
}

const ROOMS_PER_PAGE = 9

function RoomNumbersPagination({ page, totalPages, total, onPageChange }) {
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-center gap-0.5 border-t py-1">
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="h-6 w-6"
        disabled={page <= 1}
        aria-label="Previous page"
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft className="h-3.5 w-3.5" />
      </Button>
      <span className="min-w-[4.5rem] text-center text-[10px] tabular-nums text-muted-foreground">
        {page}/{totalPages} · {total}
      </span>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="h-6 w-6"
        disabled={page >= totalPages}
        aria-label="Next page"
        onClick={() => onPageChange(page + 1)}
      >
        <ChevronRight className="h-3.5 w-3.5" />
      </Button>
    </div>
  )
}

export default function RoomTypeFormWizard({
  wizardStep,
  form,
  updateField,
  amenities,
  amenityInput,
  setAmenityInput,
  addAmenity,
  removeAmenity,
  imageUrls,
  removeImage,
  uploading,
  fileInputRef,
  handleImageUpload,
  selectedUnitIds,
  toggleUnit,
  selectableNumbers,
  editingRoomId = null,
  roomConfig,
  stepError,
}) {
  const preview = catalogFromWizardForm(form, amenities, imageUrls, roomConfig, form.baseNightlyRate)
  const sortedNumbers = sortByRoomNumber(selectableNumbers)
  const [roomPage, setRoomPage] = useState(1)
  const roomTotalPages = Math.max(1, Math.ceil(sortedNumbers.length / ROOMS_PER_PAGE))
  const pagedNumbers = sortedNumbers.slice((roomPage - 1) * ROOMS_PER_PAGE, roomPage * ROOMS_PER_PAGE)

  useEffect(() => {
    if (wizardStep === 0) {
      setRoomPage(1)
    }
  }, [wizardStep, sortedNumbers.length])

  useEffect(() => {
    if (roomPage > roomTotalPages) {
      setRoomPage(roomTotalPages)
    }
  }, [roomPage, roomTotalPages])

  function isAssignedElsewhere(unit) {
    return Boolean(unit.roomTypeId && unit.roomTypeId !== editingRoomId)
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="shrink-0 overflow-visible">
        <RoomTypeWizardStepper steps={WIZARD_STEPS} currentStep={wizardStep} />
      </div>

      {stepError && <StaffAlert className="mb-3 shrink-0">{stepError}</StaffAlert>}

      <div className="min-h-0 flex-1 overflow-hidden">
        <div
          className={cn(
            'h-full min-h-0',
            wizardStep === 0 ? 'overflow-hidden' : 'overflow-y-auto overscroll-contain pr-0.5'
          )}
        >
        {wizardStep === 0 && (
        <section className="flex h-full min-h-0 flex-col gap-3">
          <WizardStepHeader step={0} legend={<RoomAssignmentLegend />} />

          {sortedNumbers.length === 0 ? (
            <p className="rounded-lg border border-dashed p-6 text-sm text-muted-foreground">
              No room numbers in inventory yet. Use Add room number on the catalog page first.
            </p>
          ) : (
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg border">
              <div className="grid min-h-0 flex-1 grid-cols-2 gap-1.5 p-1.5 sm:grid-cols-3">
                {pagedNumbers.map((unit) => {
                  const locked = isAssignedElsewhere(unit)
                  const assignment = roomAssignmentStyles(unit, editingRoomId)
                  return (
                    <label
                      key={unit.id}
                      htmlFor={`unit-${unit.id}`}
                      className={cn(
                        'flex min-h-[2.75rem] min-w-0 items-start gap-2 rounded-md border px-2 py-1.5 transition-colors',
                        assignment.card,
                        locked
                          ? 'cursor-not-allowed opacity-75'
                          : 'cursor-pointer hover:bg-muted/30'
                      )}
                    >
                      <Checkbox
                        id={`unit-${unit.id}`}
                        className="mt-0.5 shrink-0"
                        disabled={locked}
                        checked={selectedUnitIds.includes(unit.id)}
                        onCheckedChange={(checked) => toggleUnit(unit.id, checked === true)}
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm font-medium leading-tight">
                          Room {unit.roomNumber}
                        </span>
                        <span className={cn('mt-0.5 block truncate text-xs leading-tight', assignment.label)}>
                          {assignmentLabel(unit, editingRoomId)}
                        </span>
                      </span>
                    </label>
                  )
                })}
              </div>
              <RoomNumbersPagination
                page={roomPage}
                totalPages={roomTotalPages}
                total={sortedNumbers.length}
                onPageChange={setRoomPage}
              />
            </div>
          )}
        </section>
      )}

      {wizardStep === 1 && (
        <section className="space-y-4">
          <WizardStepHeader step={1} />
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <RoomOptionSelect
            label="Room category"
            options={roomConfig.categories || []}
            value={form.roomCategoryId}
            onChange={(value) => updateField('roomCategoryId', value)}
          />
          <RoomOptionSelect
            label="View"
            options={roomConfig.views || []}
            value={form.roomViewId}
            onChange={(value) => updateField('roomViewId', value)}
          />
          <RoomOptionSelect
            label="Bed type"
            options={roomConfig.bedTypes || []}
            value={form.bedTypeId}
            onChange={(value) => updateField('bedTypeId', value)}
          />
          </div>
        </section>
      )}

      {wizardStep === 2 && (
        <section className="space-y-4">
          <WizardStepHeader step={2} />
          <div className="grid gap-4 lg:grid-cols-2">
          <div className="grid gap-2 lg:col-span-2">
            <Label htmlFor="name">Room name</Label>
            <Input id="name" value={form.name} onChange={(e) => updateField('name', e.target.value)} required />
          </div>
          <div className="grid gap-2 lg:col-span-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              value={form.description}
              onChange={(e) => updateField('description', e.target.value)}
              rows={4}
              placeholder="Describe the room for guests — what makes it comfortable and welcoming"
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="squareMeters">Square meters</Label>
            <Input
              id="squareMeters"
              type="number"
              min="1"
              step="0.1"
              value={form.squareMeters}
              onChange={(e) => updateField('squareMeters', e.target.value)}
              placeholder="28"
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="baseNightlyRate">Base nightly rate (PHP)</Label>
            <Input
              id="baseNightlyRate"
              type="number"
              min="0.01"
              step="0.01"
              value={form.baseNightlyRate}
              onChange={(e) => updateField('baseNightlyRate', e.target.value)}
              required
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="maxAdults">Max adults</Label>
            <Input
              id="maxAdults"
              type="number"
              min="1"
              value={form.maxAdults}
              onChange={(e) => updateField('maxAdults', e.target.value)}
              required
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="maxChildren">Max children</Label>
            <Input
              id="maxChildren"
              type="number"
              min="0"
              value={form.maxChildren}
              onChange={(e) => updateField('maxChildren', e.target.value)}
              required
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="totalCapacity">Inventory capacity</Label>
            <Input
              id="totalCapacity"
              type="number"
              min="1"
              value={form.totalCapacity}
              onChange={(e) => updateField('totalCapacity', e.target.value)}
              required
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="ratePlanName">Rate plan name</Label>
            <Input
              id="ratePlanName"
              value={form.ratePlanName}
              onChange={(e) => updateField('ratePlanName', e.target.value)}
            />
          </div>
          </div>
        </section>
      )}

      {wizardStep === 3 && (
        <section className="space-y-5">
          <WizardStepHeader step={3} />
          <div className="space-y-3">
            <Label htmlFor="amenity-input">Amenities</Label>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Input
                id="amenity-input"
                value={amenityInput}
                onChange={(e) => setAmenityInput(e.target.value)}
                placeholder="Wi-Fi, Air conditioning, Mini bar"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    addAmenity()
                  }
                }}
              />
              <Button type="button" variant="secondary" onClick={addAmenity}>
                Add
              </Button>
            </div>
            {amenities.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {amenities.map((item) => (
                  <Badge key={item} variant="secondary" className="gap-1 pr-1">
                    {item}
                    <button
                      type="button"
                      className="rounded-full p-0.5 hover:bg-muted"
                      onClick={() => removeAmenity(item)}
                      aria-label={`Remove ${item}`}
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                ))}
              </div>
            )}
          </div>

          <div className="space-y-3">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <Label>Room images</Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={uploading}
                onClick={() => fileInputRef.current?.click()}
              >
                <ImagePlus className="h-4 w-4" />
                {uploading ? 'Uploading…' : 'Upload images'}
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                multiple
                className="hidden"
                onChange={handleImageUpload}
              />
            </div>
            {imageUrls.length > 0 ? (
              <Carousel className="mx-auto w-full max-w-md">
                <CarouselContent>
                  {imageUrls.map((url) => (
                    <CarouselItem key={url}>
                      <div className="relative overflow-hidden rounded-xl border">
                        <img src={url} alt="Room preview" className="aspect-[4/3] w-full object-cover" />
                        <Button
                          type="button"
                          variant="destructive"
                          size="icon-sm"
                          className="absolute top-2 right-2"
                          onClick={() => removeImage(url)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </CarouselItem>
                  ))}
                </CarouselContent>
                {imageUrls.length > 1 && (
                  <>
                    <CarouselPrevious />
                    <CarouselNext />
                  </>
                )}
              </Carousel>
            ) : (
              <div className="flex aspect-[4/3] max-w-md items-center justify-center rounded-xl border border-dashed bg-muted/30 text-sm text-muted-foreground">
                No images uploaded yet
              </div>
            )}
          </div>
        </section>
      )}

      {wizardStep === 4 && (
        <section className="space-y-4">
          <WizardStepHeader step={4} />
          <div className="grid gap-3 md:grid-cols-2">
          <div className="flex items-center justify-between gap-4 rounded-lg border p-4">
            <div>
              <Label htmlFor="refundable">Refundable</Label>
              <p className="mt-1 text-sm text-muted-foreground">Guests can receive a refund within the policy window.</p>
            </div>
            <Switch
              id="refundable"
              checked={form.refundable}
              onCheckedChange={(value) => updateField('refundable', value)}
            />
          </div>
          <div className="flex items-center justify-between gap-4 rounded-lg border p-4">
            <div>
              <Label htmlFor="freeCancellation">Free cancellation</Label>
              <p className="mt-1 text-sm text-muted-foreground">Show free cancellation on the guest room card.</p>
            </div>
            <Switch
              id="freeCancellation"
              checked={form.freeCancellation}
              onCheckedChange={(value) => updateField('freeCancellation', value)}
            />
          </div>
          <div className="flex items-center gap-3 md:col-span-2">
            <Switch checked={form.active} onCheckedChange={(value) => updateField('active', value)} id="active" />
            <Label htmlFor="active">Active on guest booking site</Label>
          </div>
          </div>
        </section>
      )}

      {wizardStep === 5 && (
        <section className="space-y-4">
          <WizardStepHeader step={5} />
          <div className="mx-auto max-w-lg">
            <RoomCatalogCard {...preview} />
          </div>
        </section>
      )}
        </div>
      </div>
    </div>
  )
}
