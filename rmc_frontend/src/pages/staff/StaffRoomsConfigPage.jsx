import { useEffect, useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import { StaffModal } from '@/components/staff/StaffModal'
import {
  StaffTable,
  StaffTableBody,
  StaffTableCell,
  StaffTableHead,
  StaffTableHeader,
  StaffTableRow,
  StaffTableWrap,
} from '@/components/staff/StaffTable'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  createRoomConfigOption,
  deleteRoomConfigOption,
  getRoomConfigOptions,
  updateRoomConfigOption,
} from '@/staffApi'

const OPTION_SECTIONS = [
  { key: 'categories', optionType: 'ROOM_CATEGORY', title: 'Room category', description: 'Categories shown when creating a room type.' },
  { key: 'views', optionType: 'ROOM_VIEW', title: 'View', description: 'View options such as city, garden, or pool.' },
  { key: 'bedTypes', optionType: 'BED_TYPE', title: 'Bed type', description: 'Bed configurations such as queen, king, or twin.' },
  { key: 'statuses', optionType: 'ROOM_STATUS', title: 'Room status', description: 'Maintenance and operational statuses for physical room numbers.' },
]

const TABLE_CLASS =
  '[&_th]:h-10 [&_th]:px-4 [&_th]:py-3 [&_th]:text-left [&_th]:text-xs [&_th]:font-semibold [&_th]:text-foreground/80 [&_td]:px-4 [&_td]:py-3.5 [&_td]:text-left [&_td]:align-middle [&_td]:text-sm'

const TABLE_PANEL_CLASS = 'overflow-hidden rounded-sm border border-border bg-card'

const TABLE_HEADER_CLASS = 'bg-muted/70 [&_tr]:border-b [&_tr]:border-border'

function OptionSection({ section, options, onChanged, setError }) {
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
    if (!window.confirm(`Remove "${option.label}" from ${section.title.toLowerCase()}?`)) return
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
        <Button type="button" size="sm" variant="outline" onClick={openCreate}>
          <Plus className="h-3.5 w-3.5" />
          Add value
        </Button>
      </div>

      {options.length === 0 ? (
        <p className="text-sm text-muted-foreground">No values yet.</p>
      ) : (
        <div className={TABLE_PANEL_CLASS}>
          <StaffTableWrap>
            <StaffTable className={TABLE_CLASS}>
              <StaffTableHeader className={TABLE_HEADER_CLASS}>
                <StaffTableRow>
                  <StaffTableHead>Label</StaffTableHead>
                  <StaffTableHead className="w-40">Actions</StaffTableHead>
                </StaffTableRow>
              </StaffTableHeader>
              <StaffTableBody>
                {options.map((option) => (
                  <StaffTableRow key={option.id}>
                    <StaffTableCell className="font-medium">{option.label}</StaffTableCell>
                    <StaffTableCell>
                      <div className="flex gap-2">
                        <Button type="button" variant="outline" size="sm" onClick={() => openEdit(option)}>
                          <Pencil className="size-3.5" />
                          Edit
                        </Button>
                        <Button type="button" variant="ghost" size="sm" onClick={() => handleDelete(option)}>
                          <Trash2 className="size-3.5 text-destructive" />
                          Remove
                        </Button>
                      </div>
                    </StaffTableCell>
                  </StaffTableRow>
                ))}
              </StaffTableBody>
            </StaffTable>
          </StaffTableWrap>
        </div>
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
  const [config, setConfig] = useState({ categories: [], views: [], bedTypes: [], statuses: [] })
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
      description="Manage dropdown values for room category, view, bed type, and room status used across the staff portal."
    >
      <StaffAlert variant="success">{message}</StaffAlert>
      <StaffAlert>{error}</StaffAlert>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="gap-4">
        <TabsList variant="line" className="h-8 w-fit max-w-full self-start rounded-none border-b bg-transparent p-0">
          {OPTION_SECTIONS.map((section) => (
            <TabsTrigger key={section.key} value={section.key} className="h-8 rounded-none px-3 text-xs sm:text-sm">
              {section.title}
            </TabsTrigger>
          ))}
        </TabsList>

        {OPTION_SECTIONS.map((section) => (
          <TabsContent key={section.key} value={section.key} className="mt-4 space-y-3">
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
          </TabsContent>
        ))}
      </Tabs>
    </StaffPage>
  )
}
