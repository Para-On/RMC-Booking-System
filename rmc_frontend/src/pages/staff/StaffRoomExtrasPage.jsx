import { useEffect, useState } from 'react'
import { Eye, Pencil, Plus, Trash2 } from 'lucide-react'
import { ServiceAddonCard } from '@/components/extras/ServiceAddonCard'
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
import { Textarea } from '@/components/ui/textarea'
import { formatMoney } from '@/api'
import {
  createStaffItemAddon,
  createStaffServiceAddon,
  deleteStaffItemAddon,
  deleteStaffServiceAddon,
  listStaffItemAddons,
  listStaffServiceAddons,
  updateStaffItemAddon,
  updateStaffServiceAddon,
  uploadItemAddonImage,
  uploadServiceAddonImage,
} from '@/staffApi'

const EMPTY_SERVICE = {
  title: '',
  subtitle: '',
  details: '',
  imageUrl: '',
  pricingMode: 'free',
  price: '',
  active: true,
}

const EMPTY_ITEM = {
  name: '',
  subtitle: '',
  details: '',
  imageUrl: '',
  pricingMode: 'free',
  price: '',
  active: true,
}

function ServiceFormFields({ form, setForm, uploading, onImageUpload }) {
  return (
    <div className="grid gap-4">
      <div className="space-y-2">
        <Label>Image</Label>
        {form.imageUrl ? (
          <img
            src={form.imageUrl}
            alt=""
            className="h-24 w-32 rounded-md border object-cover"
          />
        ) : null}
        <Input
          type="file"
          accept="image/png,image/jpeg,image/webp,image/svg+xml"
          onChange={onImageUpload}
          disabled={uploading}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="service-title">Title</Label>
        <Input
          id="service-title"
          value={form.title}
          onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="service-subtitle">Subtitle</Label>
        <Input
          id="service-subtitle"
          value={form.subtitle}
          onChange={(e) => setForm((prev) => ({ ...prev, subtitle: e.target.value }))}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="service-details">Details</Label>
        <Textarea
          id="service-details"
          value={form.details}
          onChange={(e) => setForm((prev) => ({ ...prev, details: e.target.value }))}
          rows={3}
        />
      </div>
      <fieldset className="space-y-2 rounded-md border p-3">
        <legend className="px-1 text-sm font-medium">Pricing</legend>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="radio"
            name="pricingMode"
            checked={form.pricingMode === 'free'}
            onChange={() => setForm((prev) => ({ ...prev, pricingMode: 'free', price: '' }))}
          />
          Free
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="radio"
            name="pricingMode"
            checked={form.pricingMode === 'price'}
            onChange={() => setForm((prev) => ({ ...prev, pricingMode: 'price' }))}
          />
          Price
        </label>
        {form.pricingMode === 'price' ? (
          <Input
            type="number"
            min="0"
            step="0.01"
            placeholder="0.00"
            value={form.price}
            onChange={(e) => setForm((prev) => ({ ...prev, price: e.target.value }))}
          />
        ) : null}
      </fieldset>
      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={form.active}
          onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
        />
        Active (visible at checkout)
      </label>
    </div>
  )
}

function ItemFormFields({ form, setForm, uploading, onImageUpload }) {
  return (
    <div className="grid gap-4">
      <div className="space-y-2">
        <Label>Image</Label>
        {form.imageUrl ? (
          <img
            src={form.imageUrl}
            alt=""
            className="h-24 w-32 rounded-md border object-cover"
          />
        ) : null}
        <Input
          type="file"
          accept="image/png,image/jpeg,image/webp,image/svg+xml"
          onChange={onImageUpload}
          disabled={uploading}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="item-name">Title</Label>
        <Input
          id="item-name"
          value={form.name}
          onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="item-subtitle">Subtitle</Label>
        <Input
          id="item-subtitle"
          value={form.subtitle}
          onChange={(e) => setForm((prev) => ({ ...prev, subtitle: e.target.value }))}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="item-details">Details</Label>
        <Textarea
          id="item-details"
          value={form.details}
          onChange={(e) => setForm((prev) => ({ ...prev, details: e.target.value }))}
          rows={3}
        />
      </div>
      <fieldset className="space-y-2 rounded-md border p-3">
        <legend className="px-1 text-sm font-medium">Pricing</legend>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="radio"
            name="itemPricingMode"
            checked={form.pricingMode === 'free'}
            onChange={() => setForm((prev) => ({ ...prev, pricingMode: 'free', price: '' }))}
          />
          Free
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="radio"
            name="itemPricingMode"
            checked={form.pricingMode === 'price'}
            onChange={() => setForm((prev) => ({ ...prev, pricingMode: 'price' }))}
          />
          Price
        </label>
        {form.pricingMode === 'price' ? (
          <Input
            type="number"
            min="0"
            step="0.01"
            placeholder="0.00"
            value={form.price}
            onChange={(e) => setForm((prev) => ({ ...prev, price: e.target.value }))}
          />
        ) : null}
      </fieldset>
      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={form.active}
          onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
        />
        Active (visible at checkout)
      </label>
    </div>
  )
}

function buildServicePayload(form) {
  const free = form.pricingMode === 'free'
  return {
    title: form.title.trim(),
    subtitle: form.subtitle.trim() || null,
    details: form.details.trim() || null,
    imageUrl: form.imageUrl || null,
    free,
    price: free ? null : Number(form.price),
    active: form.active,
  }
}

function buildItemPayload(form) {
  const free = form.pricingMode === 'free'
  return {
    name: form.name.trim(),
    subtitle: form.subtitle.trim() || null,
    details: form.details.trim() || null,
    imageUrl: form.imageUrl || null,
    free,
    price: free ? null : Number(form.price),
    active: form.active,
  }
}

function serviceToForm(service) {
  return {
    title: service.title,
    subtitle: service.subtitle || '',
    details: service.details || '',
    imageUrl: service.imageUrl || '',
    pricingMode: service.free ? 'free' : 'price',
    price: service.price != null ? String(service.price) : '',
    active: service.active,
    sortOrder: service.sortOrder,
  }
}

function itemToForm(item) {
  return {
    name: item.name,
    subtitle: item.subtitle || '',
    details: item.details || '',
    imageUrl: item.imageUrl || '',
    pricingMode: item.free ? 'free' : 'price',
    price: item.price != null ? String(item.price) : '',
    active: item.active,
    sortOrder: item.sortOrder,
  }
}

function itemAsServiceCard(item) {
  return {
    id: item.id,
    title: item.name,
    subtitle: item.subtitle,
    details: item.details,
    imageUrl: item.imageUrl,
    free: item.free,
    price: item.price,
  }
}

export default function StaffRoomExtrasPage() {
  const [tab, setTab] = useState('services')
  const [services, setServices] = useState([])
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [serviceForm, setServiceForm] = useState(EMPTY_SERVICE)
  const [serviceCreateOpen, setServiceCreateOpen] = useState(false)
  const [serviceEditOpen, setServiceEditOpen] = useState(false)
  const [servicePreview, setServicePreview] = useState(null)
  const [editingService, setEditingService] = useState(null)
  const [uploadingImage, setUploadingImage] = useState(false)
  const [savingService, setSavingService] = useState(false)

  const [itemForm, setItemForm] = useState(EMPTY_ITEM)
  const [itemCreateOpen, setItemCreateOpen] = useState(false)
  const [itemEditOpen, setItemEditOpen] = useState(false)
  const [itemPreview, setItemPreview] = useState(null)
  const [editingItem, setEditingItem] = useState(null)
  const [savingItem, setSavingItem] = useState(false)

  useEffect(() => {
    loadAll()
  }, [])

  async function loadAll() {
    setLoading(true)
    setError('')
    try {
      const [serviceData, itemData] = await Promise.all([
        listStaffServiceAddons(),
        listStaffItemAddons(),
      ])
      setServices(serviceData)
      setItems(itemData)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleServiceImageUpload(e, setForm) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploadingImage(true)
    setError('')
    try {
      const result = await uploadServiceAddonImage(file)
      setForm((prev) => ({ ...prev, imageUrl: result.assetUrl }))
    } catch (err) {
      setError(err.message)
    } finally {
      setUploadingImage(false)
      e.target.value = ''
    }
  }

  async function handleItemImageUpload(e, setForm) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploadingImage(true)
    setError('')
    try {
      const result = await uploadItemAddonImage(file)
      setForm((prev) => ({ ...prev, imageUrl: result.assetUrl }))
    } catch (err) {
      setError(err.message)
    } finally {
      setUploadingImage(false)
      e.target.value = ''
    }
  }

  async function handleCreateService() {
    if (!serviceForm.title.trim()) return
    setSavingService(true)
    setError('')
    try {
      await createStaffServiceAddon(buildServicePayload(serviceForm))
      setServiceCreateOpen(false)
      setServiceForm(EMPTY_SERVICE)
      setMessage('Service add-on created')
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingService(false)
    }
  }

  async function handleUpdateService() {
    if (!editingService) return
    setSavingService(true)
    setError('')
    try {
      const payload = {
        ...buildServicePayload(serviceForm),
        sortOrder: serviceForm.sortOrder ?? editingService.sortOrder,
      }
      await updateStaffServiceAddon(editingService.id, payload)
      setServiceEditOpen(false)
      setEditingService(null)
      setMessage('Service add-on updated')
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingService(false)
    }
  }

  async function handleDeleteService(service) {
    if (!window.confirm(`Delete service "${service.title}"?`)) return
    setError('')
    try {
      const result = await deleteStaffServiceAddon(service.id)
      setMessage(result?.message || 'Service add-on deleted')
      await loadAll()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCreateItem() {
    if (!itemForm.name.trim()) return
    setSavingItem(true)
    setError('')
    try {
      await createStaffItemAddon(buildItemPayload(itemForm))
      setItemCreateOpen(false)
      setItemForm(EMPTY_ITEM)
      setMessage('Item add-on created')
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingItem(false)
    }
  }

  async function handleUpdateItem() {
    if (!editingItem) return
    setSavingItem(true)
    setError('')
    try {
      await updateStaffItemAddon(editingItem.id, {
        ...buildItemPayload(itemForm),
        sortOrder: itemForm.sortOrder ?? editingItem.sortOrder,
      })
      setItemEditOpen(false)
      setEditingItem(null)
      setMessage('Item add-on updated')
      await loadAll()
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingItem(false)
    }
  }

  async function handleDeleteItem(item) {
    if (!window.confirm(`Delete item "${item.name}"?`)) return
    setError('')
    try {
      const result = await deleteStaffItemAddon(item.id)
      setMessage(result?.message || 'Item add-on deleted')
      await loadAll()
    } catch (err) {
      setError(err.message)
    }
  }

  function previewService(service) {
    setServicePreview({
      id: service.id,
      title: service.title,
      subtitle: service.subtitle,
      details: service.details,
      imageUrl: service.imageUrl,
      free: service.free,
      price: service.price,
    })
  }

  if (loading) {
    return (
      <StaffPage title="Extras">
        <p className="text-sm text-muted-foreground">Loading extras…</p>
      </StaffPage>
    )
  }

  return (
    <StaffPage
      title="Extras"
      description="Configure optional service and item add-ons shown on the guest checkout page."
    >
      {error && <StaffAlert>{error}</StaffAlert>}
      {message && <StaffAlert variant="success">{message}</StaffAlert>}

      <StaffPageTabs value={tab} onValueChange={setTab}>
        <StaffPageTabList>
          <StaffPageTabTrigger value="services">Services</StaffPageTabTrigger>
          <StaffPageTabTrigger value="items">Items</StaffPageTabTrigger>
        </StaffPageTabList>

        <StaffPageTabContent value="services" className="space-y-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Paid or free services guests can add to cart at checkout. Use preview to see the
              horizontal card layout.
            </p>
            <Button type="button" size="sm" onClick={() => setServiceCreateOpen(true)}>
              <Plus className="h-3.5 w-3.5" />
              Add service
            </Button>
          </div>

          {services.length === 0 ? (
            <p className="text-sm text-muted-foreground">No service add-ons yet.</p>
          ) : (
            <StaffTablePanel>
              <StaffTableWrap>
                <StaffTable>
                  <StaffTableHeader>
                    <StaffTableRow>
                      <StaffTableHead>Title</StaffTableHead>
                      <StaffTableHead>Price</StaffTableHead>
                      <StaffTableHead>Status</StaffTableHead>
                      <StaffTableActionsHead />
                    </StaffTableRow>
                  </StaffTableHeader>
                  <StaffTableBody>
                    {services.map((service) => (
                      <StaffTableRow key={service.id}>
                        <StaffTableCell>
                          <div className="font-medium">{service.title}</div>
                          {service.subtitle ? (
                            <div className="text-xs text-muted-foreground">{service.subtitle}</div>
                          ) : null}
                        </StaffTableCell>
                        <StaffTableCell>
                          {service.free ? 'Free' : formatMoney(service.price, 'PHP')}
                        </StaffTableCell>
                        <StaffTableCell>{service.active ? 'Active' : 'Hidden'}</StaffTableCell>
                        <StaffTableActionsCell>
                          <StaffTableRowActions label={`Actions for ${service.title}`}>
                            <StaffTableAction icon={Eye} onClick={() => previewService(service)}>
                              Preview
                            </StaffTableAction>
                            <StaffTableAction
                              icon={Pencil}
                              onClick={() => {
                                setEditingService(service)
                                setServiceForm(serviceToForm(service))
                                setServiceEditOpen(true)
                              }}
                            >
                              Edit
                            </StaffTableAction>
                            <StaffTableActionSeparator />
                            <StaffTableAction
                              icon={Trash2}
                              variant="destructive"
                              onClick={() => handleDeleteService(service)}
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
            </StaffTablePanel>
          )}
        </StaffPageTabContent>

        <StaffPageTabContent value="items" className="space-y-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Paid or free items guests can add to cart at checkout. Use preview to see the
              horizontal card layout.
            </p>
            <Button
              type="button"
              size="sm"
              onClick={() => {
                setItemForm(EMPTY_ITEM)
                setItemCreateOpen(true)
              }}
            >
              <Plus className="h-3.5 w-3.5" />
              Add item
            </Button>
          </div>

          {items.length === 0 ? (
            <p className="text-sm text-muted-foreground">No item add-ons yet.</p>
          ) : (
            <StaffTablePanel>
              <StaffTableWrap>
                <StaffTable>
                  <StaffTableHeader>
                    <StaffTableRow>
                      <StaffTableHead>Title</StaffTableHead>
                      <StaffTableHead>Price</StaffTableHead>
                      <StaffTableHead>Status</StaffTableHead>
                      <StaffTableActionsHead />
                    </StaffTableRow>
                  </StaffTableHeader>
                  <StaffTableBody>
                    {items.map((item) => (
                      <StaffTableRow key={item.id}>
                        <StaffTableCell>
                          <div className="font-medium">{item.name}</div>
                          {item.subtitle ? (
                            <div className="text-xs text-muted-foreground">{item.subtitle}</div>
                          ) : null}
                        </StaffTableCell>
                        <StaffTableCell>
                          {item.free ? 'Free' : formatMoney(item.price, 'PHP')}
                        </StaffTableCell>
                        <StaffTableCell>{item.active ? 'Active' : 'Hidden'}</StaffTableCell>
                        <StaffTableActionsCell>
                          <StaffTableRowActions label={`Actions for ${item.name}`}>
                            <StaffTableAction
                              icon={Eye}
                              onClick={() => setItemPreview(itemAsServiceCard(item))}
                            >
                              Preview
                            </StaffTableAction>
                            <StaffTableAction
                              icon={Pencil}
                              onClick={() => {
                                setEditingItem(item)
                                setItemForm(itemToForm(item))
                                setItemEditOpen(true)
                              }}
                            >
                              Edit
                            </StaffTableAction>
                            <StaffTableActionSeparator />
                            <StaffTableAction
                              icon={Trash2}
                              variant="destructive"
                              onClick={() => handleDeleteItem(item)}
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
            </StaffTablePanel>
          )}
        </StaffPageTabContent>
      </StaffPageTabs>

      <StaffModal
        open={serviceCreateOpen}
        onOpenChange={setServiceCreateOpen}
        title="Create service add-on"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setServiceCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleCreateService} disabled={savingService}>
              {savingService ? 'Saving…' : 'Create'}
            </Button>
          </>
        }
      >
        <ServiceFormFields
          form={serviceForm}
          setForm={setServiceForm}
          uploading={uploadingImage}
          onImageUpload={(e) => handleServiceImageUpload(e, setServiceForm)}
        />
      </StaffModal>

      <StaffModal
        open={serviceEditOpen}
        onOpenChange={setServiceEditOpen}
        title="Edit service add-on"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setServiceEditOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleUpdateService} disabled={savingService}>
              {savingService ? 'Saving…' : 'Save'}
            </Button>
          </>
        }
      >
        <ServiceFormFields
          form={serviceForm}
          setForm={setServiceForm}
          uploading={uploadingImage}
          onImageUpload={(e) => handleServiceImageUpload(e, setServiceForm)}
        />
      </StaffModal>

      <StaffModal
        open={Boolean(servicePreview)}
        onOpenChange={(open) => !open && setServicePreview(null)}
        title="Checkout card preview"
        footer={
          <Button type="button" variant="outline" onClick={() => setServicePreview(null)}>
            Close
          </Button>
        }
      >
        {servicePreview ? (
          <ServiceAddonCard
            service={servicePreview}
            onAddToCart={() => setMessage('Preview only — add to cart works on checkout')}
          />
        ) : null}
      </StaffModal>

      <StaffModal
        open={itemCreateOpen}
        onOpenChange={setItemCreateOpen}
        title="Create item add-on"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setItemCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleCreateItem} disabled={savingItem}>
              {savingItem ? 'Saving…' : 'Create'}
            </Button>
          </>
        }
      >
        <ItemFormFields
          form={itemForm}
          setForm={setItemForm}
          uploading={uploadingImage}
          onImageUpload={(e) => handleItemImageUpload(e, setItemForm)}
        />
      </StaffModal>

      <StaffModal
        open={itemEditOpen}
        onOpenChange={setItemEditOpen}
        title="Edit item add-on"
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setItemEditOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={handleUpdateItem} disabled={savingItem}>
              {savingItem ? 'Saving…' : 'Save'}
            </Button>
          </>
        }
      >
        <ItemFormFields
          form={itemForm}
          setForm={setItemForm}
          uploading={uploadingImage}
          onImageUpload={(e) => handleItemImageUpload(e, setItemForm)}
        />
      </StaffModal>

      <StaffModal
        open={Boolean(itemPreview)}
        onOpenChange={(open) => !open && setItemPreview(null)}
        title="Checkout card preview"
        footer={
          <Button type="button" variant="outline" onClick={() => setItemPreview(null)}>
            Close
          </Button>
        }
      >
        {itemPreview ? (
          <ServiceAddonCard
            service={itemPreview}
            onAddToCart={() => setMessage('Preview only — add to cart works on checkout')}
          />
        ) : null}
      </StaffModal>
    </StaffPage>
  )
}
