import { useEffect, useState } from 'react'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import {
  StaffTable,
  StaffTableAction,
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
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { KeyRound, UserCheck, UserX } from 'lucide-react'
import {
  createStaffUser,
  listStaffUsers,
  resetStaffPassword,
  updateStaffUser,
} from '@/staffApi'
import { STAFF_ROLE_LABELS } from '@/staffAuth'

export default function StaffUsersPage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [newUser, setNewUser] = useState({
    email: '',
    fullName: '',
    role: 'FRONT_DESK',
    password: '',
  })

  useEffect(() => {
    loadUsers()
  }, [])

  async function loadUsers() {
    setLoading(true)
    setError('')
    try {
      const data = await listStaffUsers()
      setUsers(data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      await createStaffUser(newUser)
      setNewUser({ email: '', fullName: '', role: 'FRONT_DESK', password: '' })
      setMessage('Staff user created')
      await loadUsers()
    } catch (err) {
      setError(err.message)
    }
  }

  async function toggleActive(user) {
    setError('')
    try {
      await updateStaffUser(user.id, { active: !user.active, role: user.role })
      await loadUsers()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleResetPassword(user) {
    const password = window.prompt(`New password for ${user.email}`)
    if (!password) return
    setError('')
    try {
      await resetStaffPassword(user.id, password)
      setMessage(`Password reset for ${user.email}`)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <StaffPageShell
      title="Staff accounts"
      description="Create and manage front desk, manager, and admin accounts."
    >
      <StaffAlert variant="success">{message}</StaffAlert>
      <StaffAlert>{error}</StaffAlert>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Add staff user</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4 sm:grid-cols-2" onSubmit={handleCreate}>
            <div className="grid gap-2">
              <Label htmlFor="user-email">Email</Label>
              <Input
                id="user-email"
                type="email"
                value={newUser.email}
                onChange={(e) => setNewUser((u) => ({ ...u, email: e.target.value }))}
                required
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="user-name">Full name</Label>
              <Input
                id="user-name"
                value={newUser.fullName}
                onChange={(e) => setNewUser((u) => ({ ...u, fullName: e.target.value }))}
                required
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="user-role">Role</Label>
              <Select
                value={newUser.role}
                onValueChange={(role) => setNewUser((u) => ({ ...u, role }))}
              >
                <SelectTrigger id="user-role">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="FRONT_DESK">{STAFF_ROLE_LABELS.FRONT_DESK}</SelectItem>
                  <SelectItem value="MANAGER">{STAFF_ROLE_LABELS.MANAGER}</SelectItem>
                  <SelectItem value="ADMIN">{STAFF_ROLE_LABELS.ADMIN}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="user-password">Temporary password</Label>
              <Input
                id="user-password"
                type="password"
                value={newUser.password}
                onChange={(e) => setNewUser((u) => ({ ...u, password: e.target.value }))}
                required
                minLength={8}
              />
            </div>
            <div className="sm:col-span-2">
              <Button type="submit">Create account</Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {loading && <p className="text-sm text-muted-foreground">Loading staff…</p>}

      {!loading && (
        <StaffTablePanel>
          <StaffTableWrap>
            <StaffTable>
              <StaffTableHeader>
                <StaffTableRow>
                  <StaffTableHead>Name</StaffTableHead>
                  <StaffTableHead>Email</StaffTableHead>
                  <StaffTableHead className="hidden sm:table-cell">Role</StaffTableHead>
                  <StaffTableHead>Active</StaffTableHead>
                  <StaffTableHead className="hidden md:table-cell">MFA</StaffTableHead>
                  <StaffTableActionsHead />
                </StaffTableRow>
              </StaffTableHeader>
              <StaffTableBody>
                {users.map((user) => (
                  <StaffTableRow key={user.id}>
                    <StaffTableCell className="font-medium">{user.fullName}</StaffTableCell>
                    <StaffTableCell>
                      <div>{user.email}</div>
                      <div className="text-xs text-muted-foreground sm:hidden">
                        {STAFF_ROLE_LABELS[user.role] || user.role}
                      </div>
                    </StaffTableCell>
                    <StaffTableCell className="hidden sm:table-cell">
                      {STAFF_ROLE_LABELS[user.role] || user.role}
                    </StaffTableCell>
                    <StaffTableCell>{user.active ? 'Yes' : 'No'}</StaffTableCell>
                    <StaffTableCell className="hidden md:table-cell">
                      {user.mfaEnabled ? 'On' : 'Off'}
                    </StaffTableCell>
                    <StaffTableActionsCell>
                      <StaffTableRowActions label={`Actions for ${user.email}`}>
                        <StaffTableAction
                          icon={user.active ? UserX : UserCheck}
                          onClick={() => toggleActive(user)}
                        >
                          {user.active ? 'Disable account' : 'Enable account'}
                        </StaffTableAction>
                        <StaffTableAction icon={KeyRound} onClick={() => handleResetPassword(user)}>
                          Reset password
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
    </StaffPageShell>
  )
}
