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
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  StaffPageTabContent,
  StaffPageTabList,
  StaffPageTabs,
  StaffPageTabTrigger,
} from '@/components/staff/StaffPageTabs'
import {
  createRoomConfigOption,
  deleteRoomConfigOption,
  getRoomConfigOptions,
  updateRoomConfigOption,
} from '@/staffApi'
import { useAppFeedback } from '@/context/AppFeedbackProvider'

const OPTION_SECTIONS = [
  { key: 'categories', optionType: 'ROOM_CATEGORY', title: 'Room category', description: 'Categories shown when creating a room type.' },
  { key: 'views', optionType: 'ROOM_VIEW', title: 'View', description: 'View options such as city, garden, or pool.' },
  { key: 'bedTypes', optionType: 'BED_TYPE', title: 'Bed type', description: 'Bed configurations such as queen, king, or twin.' },
  { key: 'amenities', optionType: 'AMENITY', title: 'Amenities', description: 'Amenity labels staff pick when creating or editing a room type (Wi‑Fi, mini bar, etc.).' },
  { key: 'statuses', optionType: 'ROOM_STATUS', title: 'Room status', description: 'Maintenance and operational statuses for physical room numbers.' },
]

function OptionSection({ section, options, onChanged, setError }) {
  const { confirm } = useAppFeedback()
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [label, setLabel] = useState('')
  const [saving, setSaving] = useState(false)

  function openCreate() {
    setLabel('')
    setCreateOpen(true)
  }

  function openEdit(option) {
    setEditing(option)
    setLabel(option.label)
    setEditOpen(true)
  }

  async function handleCreate() {
    const trimmed = label.trim()
    if (!trimmed) return
    setSaving(true)
    setError('')
    try {
      await createRoomConfigOption(section.optionType, trimmed)
      setCreateOpen(false)
      await onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate() {
    if (!editing) return
    const trimmed = label.trim()
    if (!trimmed) return
    setSaving(true)
    setError('')
    try {
      await updateRoomConfigOption(editing.id, trimmed)
      setEditOpen(false)
      setEditing(null)
      await onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(option) {
    const decision = await confirm({
      title: `Remove ${section.title.toLowerCase()}?`,
      description: `Remove "${option.label}" from ${section.title.toLowerCase()}?`,
      confirmLabel: 'Remove',
      variant: 'destructive',
    })
    if (!decision.confirmed) return
    setError('')
    try {
      await deleteRoomConfigOption(option.id)
      await onChanged()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-muted-foreground">{section.description}</p>
        <Button type="button" size="sm" onClick={openCreate}>
          <Plus className="h-3.5 w-3.5" />
          Add value
        </Button>
      </div>

      {options.length === 0 ? (
        <p className="text-sm text-muted-foreground">No values yet.</p>
      ) : (
        <StaffTablePanel>
          <StaffTableWrap>
            <StaffTable>
              <StaffTableHeader>
                <StaffTableRow>
                  <StaffTableHead>Label</StaffTableHead>
                  <StaffTableActionsHead />
                </StaffTableRow>
              </StaffTableHeader>
              <StaffTableBody>
                {options.map((option) => (
                  <StaffTableRow key={option.id}>
                    <StaffTableCell className="font-medium">{option.label}</StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions label={`Actions for ${option.label}`}>
                        <StaffTableAction icon={Pencil} onClick={() => openEdit(option)}>
                          Edit
                        </StaffTableAction>
                        <StaffTableActionSeparator />
                        <StaffTableAction
                          icon={Trash2}
                          variant="destructive"
                          onClick={() => handleDelete(option)}
                        >
                          Remove
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
        open={createOpen}
        onOpenChange={setCreateOpen}
        title={`Add ${section.title.toLowerCase()}`}
        size="sm"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleCreate} disabled={saving || !label.trim()}>
              {saving ? 'Saving…' : 'Add'}
            </Button>
          </>
        }
      >
        <div className="grid gap-2">
          <Label htmlFor={`${section.key}-create`}>Label</Label>
          <Input
            id={`${section.key}-create`}
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            placeholder={`Enter ${section.title.toLowerCase()}`}
            autoFocus
          />
        </div>
      </StaffModal>

      <StaffModal
        open={editOpen}
        onOpenChange={setEditOpen}
        title={`Edit ${section.title.toLowerCase()}`}
        size="sm"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setEditOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleUpdate} disabled={saving || !label.trim()}>
              {saving ? 'Saving…' : 'Save'}
            </Button>
          </>
        }
      >
        <div className="grid gap-2">
          <Label htmlFor={`${section.key}-edit`}>Label</Label>
          <Input
            id={`${section.key}-edit`}
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            autoFocus
          />
        </div>
      </StaffModal>
    </>
  )
}

export default function StaffRoomsConfigPage() {
  const [config, setConfig] = useState({
    categories: [],
    views: [],
    bedTypes: [],
    amenities: [],
    statuses: [],
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [activeTab, setActiveTab] = useState('categories')

  useEffect(() => {
    loadConfig()
  }, [])

  async function loadConfig() {
    setLoading(true)
    setError('')
    try {
      const data = await getRoomConfigOptions()
      setConfig(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleChanged() {
    setMessage('Room configuration updated.')
    await loadConfig()
  }

  return (
    <StaffPage
      title="Room configuration"
      description="Manage dropdown values for room category, view, bed type, amenities, and room status used across the staff portal."
    >
      <StaffAlert variant="success">{message}</StaffAlert>
      <StaffAlert>{error}</StaffAlert>

      <StaffPageTabs value={activeTab} onValueChange={setActiveTab}>
        <StaffPageTabList>
          {OPTION_SECTIONS.map((section) => (
            <StaffPageTabTrigger key={section.key} value={section.key}>
              {section.title}
            </StaffPageTabTrigger>
          ))}
        </StaffPageTabList>

        {OPTION_SECTIONS.map((section) => (
          <StaffPageTabContent key={section.key} value={section.key} className="space-y-3">
            {loading ? (
              <p className="text-sm text-muted-foreground">Loading configuration…</p>
            ) : (
              <OptionSection
                section={section}
                options={config[section.key] || []}
                onChanged={handleChanged}
                setError={setError}
              />
            )}
          </StaffPageTabContent>
        ))}
      </StaffPageTabs>
    </StaffPage>
  )
}
