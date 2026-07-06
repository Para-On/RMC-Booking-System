import { useEffect, useMemo, useState } from 'react'
import { ArrowDown, ArrowUp, GripVertical, Pencil, Plus, Trash2 } from 'lucide-react'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import { StaffModal } from '@/components/staff/StaffModal'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
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
import { Separator } from '@/components/ui/separator'
import { Switch } from '@/components/ui/switch'
import { STAFF_NAV_ICONS } from '@/config/staffModules'
import {
  createStaffNavModule,
  deleteStaffNavModule,
  getAllStaffNavModules,
  getStaffNavRoles,
  updateStaffNavModule,
  updateStaffNavModules,
} from '@/staffApi'

function notifyNavChanged() {
  window.dispatchEvent(new Event('staff-nav-changed'))
}

const EMPTY_FORM = {
  moduleKey: '',
  label: '',
  path: '/staff/',
  icon: 'layout',
  allowedRoles: [],
  parentId: '',
  enabled: true,
}

function flattenForSave(modules) {
  const items = []
  modules.forEach((module, rootIndex) => {
    items.push({
      id: module.id,
      enabled: module.enabled,
      sortOrder: (rootIndex + 1) * 10,
    })
    ;(module.children || []).forEach((child, childIndex) => {
      items.push({
        id: child.id,
        enabled: child.enabled,
        sortOrder: (childIndex + 1) * 10,
      })
    })
  })
  return items
}

function RoleCheckboxes({ roles, selected, onChange }) {
  return (
    <div className="space-y-2 rounded-lg border p-3">
      {roles.map((role) => (
        <div key={role.value} className="flex items-center gap-2">
          <Checkbox
            id={`role-${role.value}`}
            checked={selected.includes(role.value)}
            onCheckedChange={(checked) => {
              onChange(
                checked
                  ? [...selected, role.value]
                  : selected.filter((value) => value !== role.value)
              )
            }}
          />
          <Label htmlFor={`role-${role.value}`} className="font-normal">
            {role.label}
          </Label>
        </div>
      ))}
    </div>
  )
}

function ModuleFormFields({ form, setForm, roles, parentOptions, isEdit }) {
  return (
    <div className="space-y-4">
      {!isEdit && (
        <div className="space-y-2">
          <Label htmlFor="module-key">Module key</Label>
          <Input
            id="module-key"
            value={form.moduleKey}
            onChange={(event) => setForm((prev) => ({ ...prev, moduleKey: event.target.value }))}
            placeholder="reports"
          />
          <p className="text-xs text-muted-foreground">Unique identifier used internally.</p>
        </div>
      )}

      {!isEdit && parentOptions.length > 0 && (
        <div className="space-y-2">
          <Label>Parent module</Label>
          <Select
            value={form.parentId || 'none'}
            onValueChange={(value) =>
              setForm((prev) => ({
                ...prev,
                parentId: value === 'none' ? '' : value,
                path: value === 'none' ? prev.path : prev.path || '/staff/',
              }))
            }
          >
            <SelectTrigger>
              <SelectValue placeholder="Top-level module" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="none">Top-level module</SelectItem>
              {parentOptions.map((option) => (
                <SelectItem key={option.id} value={String(option.id)}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="module-label">Label</Label>
        <Input
          id="module-label"
          value={form.label}
          onChange={(event) => setForm((prev) => ({ ...prev, label: event.target.value }))}
          placeholder="Reports"
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="module-path">Path</Label>
        <Input
          id="module-path"
          value={form.path}
          onChange={(event) => setForm((prev) => ({ ...prev, path: event.target.value }))}
          placeholder={form.parentId ? '/staff/reports' : '/staff/reports or # for group header'}
        />
        <p className="text-xs text-muted-foreground">
          Use <code>#</code> for a top-level group with no direct page. Sub-modules must use a{' '}
          <code>/staff/…</code> path.
        </p>
      </div>

      <div className="space-y-2">
        <Label>Icon</Label>
        <Select
          value={form.icon}
          onValueChange={(value) => setForm((prev) => ({ ...prev, icon: value }))}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STAFF_NAV_ICONS.map((icon) => (
              <SelectItem key={icon.value} value={icon.value}>
                {icon.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label>Allowed roles</Label>
        <RoleCheckboxes
          roles={roles}
          selected={form.allowedRoles}
          onChange={(allowedRoles) => setForm((prev) => ({ ...prev, allowedRoles }))}
        />
      </div>

      <div className="flex items-center gap-2">
        <Switch
          id="module-enabled"
          checked={form.enabled}
          onCheckedChange={(enabled) => setForm((prev) => ({ ...prev, enabled }))}
        />
        <Label htmlFor="module-enabled" className="font-normal">
          Visible in sidebar
        </Label>
      </div>
    </div>
  )
}

function ModuleRow({ module, index, total, depth, onMove, onToggle, onEdit, onDelete, onAddChild }) {
  const isChild = depth > 0

  return (
    <div className={isChild ? 'ml-6 border-l pl-4' : ''}>
      <div className="flex flex-col gap-4 rounded-xl border bg-card p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-start gap-3">
          <GripVertical className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
          <div className="min-w-0 space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <p className="font-medium">{module.label}</p>
              {isChild && <Badge variant="secondary">Sub-module</Badge>}
            </div>
            <p className="truncate text-sm text-muted-foreground">{module.path}</p>
            <div className="flex flex-wrap gap-1 pt-1">
              {(module.allowedRoles || []).map((role) => (
                <Badge key={role} variant="outline">
                  {role}
                </Badge>
              ))}
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 sm:gap-3">
          <div className="flex items-center gap-1">
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={() => onMove(module.id, 'up', depth)}
              disabled={index === 0}
              aria-label={`Move ${module.label} up`}
            >
              <ArrowUp />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={() => onMove(module.id, 'down', depth)}
              disabled={index === total - 1}
              aria-label={`Move ${module.label} down`}
            >
              <ArrowDown />
            </Button>
          </div>
          <Separator orientation="vertical" className="hidden h-6 sm:block" />
          <div className="flex items-center gap-2">
            <Switch
              id={`module-${module.id}`}
              checked={module.enabled}
              onCheckedChange={(enabled) => onToggle(module.id, { enabled })}
            />
            <Label htmlFor={`module-${module.id}`} className="text-sm font-normal">
              {module.enabled ? 'Visible' : 'Hidden'}
            </Label>
          </div>
          <Button type="button" variant="outline" size="sm" onClick={() => onEdit(module)}>
            <Pencil className="size-3.5" />
            Edit
          </Button>
          {!isChild && (
            <Button type="button" variant="outline" size="sm" onClick={() => onAddChild(module)}>
              <Plus className="size-3.5" />
              Sub-module
            </Button>
          )}
          <Button type="button" variant="ghost" size="sm" onClick={() => onDelete(module)}>
            <Trash2 className="size-3.5 text-destructive" />
            Delete
          </Button>
        </div>
      </div>
    </div>
  )
}

export default function StaffModulesPage() {
  const [modules, setModules] = useState([])
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_FORM)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [editingModule, setEditingModule] = useState(null)

  const parentOptions = useMemo(() => modules.filter((module) => !module.parentId), [modules])

  useEffect(() => {
    loadPageData()
  }, [])

  async function loadPageData() {
    setLoading(true)
    setError('')
    try {
      const [moduleData, roleData] = await Promise.all([getAllStaffNavModules(), getStaffNavRoles()])
      setModules(moduleData)
      setRoles(roleData)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function updateModuleInTree(id, patch) {
    setModules((prev) =>
      prev.map((module) => {
        if (module.id === id) {
          return { ...module, ...patch }
        }
        if (module.children?.length) {
          return {
            ...module,
            children: module.children.map((child) =>
              child.id === id ? { ...child, ...patch } : child
            ),
          }
        }
        return module
      })
    )
  }

  function moveModule(id, direction, depth) {
    setModules((prev) => {
      if (depth === 0) {
        const index = prev.findIndex((item) => item.id === id)
        if (index < 0) return prev
        const target = direction === 'up' ? index - 1 : index + 1
        if (target < 0 || target >= prev.length) return prev
        const next = [...prev]
        ;[next[index], next[target]] = [next[target], next[index]]
        return next.map((item, sortOrder) => ({ ...item, sortOrder: (sortOrder + 1) * 10 }))
      }

      return prev.map((module) => {
        if (!module.children?.length) return module
        const index = module.children.findIndex((child) => child.id === id)
        if (index < 0) return module
        const target = direction === 'up' ? index - 1 : index + 1
        if (target < 0 || target >= module.children.length) return module
        const children = [...module.children]
        ;[children[index], children[target]] = [children[target], children[index]]
        return {
          ...module,
          children: children.map((child, sortOrder) => ({
            ...child,
            sortOrder: (sortOrder + 1) * 10,
          })),
        }
      })
    })
  }

  async function handleSaveOrder() {
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const updated = await updateStaffNavModules(flattenForSave(modules))
      setModules(updated)
      setMessage('Sidebar visibility and order saved.')
      notifyNavChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  function openCreate(parent = null) {
    setCreateForm({
      ...EMPTY_FORM,
      parentId: parent ? String(parent.id) : '',
      path: parent ? `/staff/${parent.moduleKey}/` : '/staff/',
      allowedRoles: roles.map((role) => role.value),
    })
    setCreateOpen(true)
  }

  function openEdit(module) {
    setEditingModule(module)
    setEditForm({
      moduleKey: module.moduleKey,
      label: module.label,
      path: module.path,
      icon: module.icon,
      allowedRoles: module.allowedRoles || [],
      parentId: module.parentId ? String(module.parentId) : '',
      enabled: module.enabled,
    })
    setEditOpen(true)
  }

  async function handleCreate() {
    setError('')
    setMessage('')
    try {
      await createStaffNavModule({
        moduleKey: createForm.moduleKey,
        label: createForm.label,
        path: createForm.path,
        icon: createForm.icon,
        allowedRoles: createForm.allowedRoles,
        parentId: createForm.parentId ? Number(createForm.parentId) : null,
        enabled: createForm.enabled,
      })
      setCreateOpen(false)
      setCreateForm(EMPTY_FORM)
      setMessage('Module created.')
      notifyNavChanged()
      await loadPageData()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleEdit() {
    if (!editingModule) return
    setError('')
    setMessage('')
    try {
      await updateStaffNavModule(editingModule.id, {
        label: editForm.label,
        path: editForm.path,
        icon: editForm.icon,
        allowedRoles: editForm.allowedRoles,
        enabled: editForm.enabled,
      })
      setEditOpen(false)
      setEditingModule(null)
      setMessage('Module updated.')
      notifyNavChanged()
      await loadPageData()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleDelete(module) {
    const childCount = module.children?.length || 0
    const promptText =
      childCount > 0
        ? `Delete "${module.label}" and its ${childCount} sub-module(s)?`
        : `Delete "${module.label}"?`
    if (!window.confirm(promptText)) return

    setError('')
    setMessage('')
    try {
      await deleteStaffNavModule(module.id)
      setMessage('Module deleted.')
      notifyNavChanged()
      await loadPageData()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <StaffPage
      title="Sidebar modules"
      description="Create modules and sub-modules, assign roles, and control sidebar visibility."
      actions={
        <Button type="button" onClick={() => openCreate()}>
          <Plus className="size-4" />
          New module
        </Button>
      }
    >
      <StaffAlert variant="success">{message}</StaffAlert>
      <StaffAlert>{error}</StaffAlert>

      <Card>
        <CardHeader>
          <CardTitle>Navigation modules</CardTitle>
          <CardDescription>
            Top-level modules appear in the sidebar. Add sub-modules under a parent to create nested
            navigation. Only selected roles will see each item.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {loading && <p className="text-sm text-muted-foreground">Loading modules…</p>}
          {!loading && modules.length === 0 && (
            <p className="text-sm text-muted-foreground">No modules yet. Create your first module.</p>
          )}
          {!loading &&
            modules.map((module, index) => (
              <div key={module.id} className="space-y-3">
                <ModuleRow
                  module={module}
                  index={index}
                  total={modules.length}
                  depth={0}
                  onMove={moveModule}
                  onToggle={updateModuleInTree}
                  onEdit={openEdit}
                  onDelete={handleDelete}
                  onAddChild={openCreate}
                />
                {(module.children || []).map((child, childIndex) => (
                  <ModuleRow
                    key={child.id}
                    module={child}
                    index={childIndex}
                    total={module.children.length}
                    depth={1}
                    onMove={moveModule}
                    onToggle={updateModuleInTree}
                    onEdit={openEdit}
                    onDelete={handleDelete}
                    onAddChild={openCreate}
                  />
                ))}
              </div>
            ))}
        </CardContent>
        {!loading && modules.length > 0 && (
          <CardFooter className="border-t">
            <Button type="button" onClick={handleSaveOrder} disabled={saving}>
              {saving ? 'Saving…' : 'Save visibility & order'}
            </Button>
          </CardFooter>
        )}
      </Card>

      <StaffModal
        open={createOpen}
        onOpenChange={setCreateOpen}
        title={createForm.parentId ? 'New sub-module' : 'New module'}
        description="Configure label, path, icon, and which staff roles can see this item."
        size="lg"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleCreate}>
              Create module
            </Button>
          </>
        }
      >
        <ModuleFormFields
          form={createForm}
          setForm={setCreateForm}
          roles={roles}
          parentOptions={parentOptions}
          isEdit={false}
        />
      </StaffModal>

      <StaffModal
        open={editOpen}
        onOpenChange={setEditOpen}
        title="Edit module"
        description="Update module details and role access."
        size="lg"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setEditOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleEdit}>
              Save changes
            </Button>
          </>
        }
      >
        <ModuleFormFields
          form={editForm}
          setForm={setEditForm}
          roles={roles}
          parentOptions={[]}
          isEdit
        />
      </StaffModal>
    </StaffPage>
  )
}
