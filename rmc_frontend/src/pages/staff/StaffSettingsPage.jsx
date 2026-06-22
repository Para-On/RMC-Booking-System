import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getManagerConfig,
  staffLogout,
  updateRatePlanConfig,
  updateRoomTypeConfig,
  updateSystemConfig,
} from '../../staffApi'
import { getStaffAuth } from '../../staffAuth'

export default function StaffSettingsPage() {
  const auth = getStaffAuth()
  const [config, setConfig] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [savingKey, setSavingKey] = useState('')

  useEffect(() => {
    loadConfig()
  }, [])

  async function loadConfig() {
    setLoading(true)
    setError('')
    try {
      const data = await getManagerConfig()
      setConfig(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function saveSystemConfig(key, value) {
    setSavingKey(`system-${key}`)
    setMessage('')
    setError('')
    try {
      const updated = await updateSystemConfig(key, value)
      setConfig((prev) => ({
        ...prev,
        systemConfig: prev.systemConfig.map((item) =>
          item.key === key ? updated : item
        ),
      }))
      setMessage(`Saved ${key}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function saveRatePlan(plan, field, rawValue) {
    setSavingKey(`rate-${plan.id}-${field}`)
    setMessage('')
    setError('')
    const payload = {
      [field]:
        field === 'active'
          ? rawValue === 'true' || rawValue === true
          : field === 'cancellationPolicy'
            ? rawValue
            : Number(rawValue),
    }
    try {
      const updated = await updateRatePlanConfig(plan.id, payload)
      setConfig((prev) => ({
        ...prev,
        ratePlans: prev.ratePlans.map((item) =>
          item.id === plan.id ? updated : item
        ),
      }))
      setMessage(`Saved rate plan ${plan.name}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  async function saveRoomType(roomType, field, rawValue) {
    setSavingKey(`room-${roomType.id}-${field}`)
    setMessage('')
    setError('')
    const payload = {
      [field]:
        field === 'active'
          ? rawValue === 'true' || rawValue === true
          : Number(rawValue),
    }
    try {
      const updated = await updateRoomTypeConfig(roomType.id, payload)
      setConfig((prev) => ({
        ...prev,
        roomTypes: prev.roomTypes.map((item) =>
          item.id === roomType.id ? updated : item
        ),
      }))
      setMessage(`Saved room type ${roomType.name}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingKey('')
    }
  }

  function handleLogout() {
    staffLogout()
    window.location.href = '/staff/login'
  }

  return (
    <div>
      <div className="staff-toolbar">
        <div>
          <h1>Manager settings</h1>
          <p className="muted">
            Signed in as {auth?.fullName} ({auth?.role})
          </p>
        </div>
        <div className="toolbar-actions">
          <Link to="/staff/arrivals" className="secondary button-link">
            Arrivals
          </Link>
          <button type="button" className="secondary" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {message && <p className="success">{message}</p>}
      {loading && <p>Loading settings…</p>}

      {!loading && config && (
        <>
          <div className="card">
            <h2>System configuration</h2>
            <div className="settings-grid">
              {config.systemConfig.map((item) => (
                <SystemConfigRow
                  key={item.key}
                  item={item}
                  saving={savingKey === `system-${item.key}`}
                  onSave={saveSystemConfig}
                />
              ))}
            </div>
          </div>

          <div className="card">
            <h2>Rate plans</h2>
            {config.ratePlans.map((plan) => (
              <div key={plan.id} className="settings-block">
                <h3>
                  {plan.name} <span className="muted">({plan.roomTypeName})</span>
                </h3>
                <label>
                  Hold TTL (minutes)
                  <input
                    type="number"
                    min="1"
                    defaultValue={plan.holdTtlMinutes}
                    onBlur={(e) => saveRatePlan(plan, 'holdTtlMinutes', e.target.value)}
                    disabled={savingKey === `rate-${plan.id}-holdTtlMinutes`}
                  />
                </label>
                <label>
                  Refund window (hours)
                  <input
                    type="number"
                    min="1"
                    defaultValue={plan.refundWindowHours}
                    onBlur={(e) => saveRatePlan(plan, 'refundWindowHours', e.target.value)}
                    disabled={savingKey === `rate-${plan.id}-refundWindowHours`}
                  />
                </label>
                <label>
                  Pay-later cutoff (hours)
                  <input
                    type="number"
                    min="0"
                    defaultValue={plan.payLaterCutoffHours ?? 0}
                    onBlur={(e) => saveRatePlan(plan, 'payLaterCutoffHours', e.target.value)}
                    disabled={savingKey === `rate-${plan.id}-payLaterCutoffHours`}
                  />
                </label>
                <label>
                  Cancellation policy
                  <textarea
                    rows={2}
                    defaultValue={plan.cancellationPolicy || ''}
                    onBlur={(e) => saveRatePlan(plan, 'cancellationPolicy', e.target.value)}
                    disabled={savingKey === `rate-${plan.id}-cancellationPolicy`}
                  />
                </label>
                <label>
                  Active
                  <select
                    defaultValue={plan.active ? 'true' : 'false'}
                    onChange={(e) => saveRatePlan(plan, 'active', e.target.value)}
                    disabled={savingKey === `rate-${plan.id}-active`}
                  >
                    <option value="true">Yes</option>
                    <option value="false">No</option>
                  </select>
                </label>
              </div>
            ))}
          </div>

          <div className="card">
            <h2>Room types</h2>
            {config.roomTypes.map((roomType) => (
              <div key={roomType.id} className="settings-block">
                <h3>{roomType.name}</h3>
                <label>
                  Overbooking buffer
                  <input
                    type="number"
                    min="0"
                    defaultValue={roomType.overbookingBuffer}
                    onBlur={(e) => saveRoomType(roomType, 'overbookingBuffer', e.target.value)}
                    disabled={savingKey === `room-${roomType.id}-overbookingBuffer`}
                  />
                </label>
                <label>
                  Min advance booking (hours)
                  <input
                    type="number"
                    min="0"
                    defaultValue={roomType.minAdvanceBookingHours}
                    onBlur={(e) =>
                      saveRoomType(roomType, 'minAdvanceBookingHours', e.target.value)
                    }
                    disabled={savingKey === `room-${roomType.id}-minAdvanceBookingHours`}
                  />
                </label>
                <label>
                  Max advance booking (days)
                  <input
                    type="number"
                    min="1"
                    defaultValue={roomType.maxAdvanceBookingDays}
                    onBlur={(e) =>
                      saveRoomType(roomType, 'maxAdvanceBookingDays', e.target.value)
                    }
                    disabled={savingKey === `room-${roomType.id}-maxAdvanceBookingDays`}
                  />
                </label>
                <label>
                  Active
                  <select
                    defaultValue={roomType.active ? 'true' : 'false'}
                    onChange={(e) => saveRoomType(roomType, 'active', e.target.value)}
                    disabled={savingKey === `room-${roomType.id}-active`}
                  >
                    <option value="true">Yes</option>
                    <option value="false">No</option>
                  </select>
                </label>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function SystemConfigRow({ item, saving, onSave }) {
  const [value, setValue] = useState(item.value)

  useEffect(() => {
    setValue(item.value)
  }, [item.value])

  return (
    <label>
      {item.key}
      {item.description && <span className="muted"> — {item.description}</span>}
      <div className="inline-field">
        <input value={value} onChange={(e) => setValue(e.target.value)} disabled={saving} />
        <button type="button" disabled={saving} onClick={() => onSave(item.key, value)}>
          {saving ? 'Saving…' : 'Save'}
        </button>
      </div>
    </label>
  )
}
