import { useEffect, useRef, useState } from 'react'
import { Camera, Moon, Sun } from 'lucide-react'
import { StaffAlert, StaffPage } from '@/components/staff/StaffPageShell'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useStaffProfile } from '@/context/StaffProfileProvider'
import { staffInitials } from '@/lib/staffProfile'
import {
  changeStaffPassword,
  updateStaffProfile,
  uploadStaffProfilePhoto,
} from '@/staffApi'
import { STAFF_ROLE_LABELS } from '@/staffAuth'

export default function StaffProfilePage() {
  const { profile, setProfile, toggleTheme, themeLoading, isDarkMode } = useStaffProfile()
  const fileInputRef = useRef(null)

  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [savingProfile, setSavingProfile] = useState(false)
  const [uploadingPhoto, setUploadingPhoto] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!profile) return
    setFullName(profile.fullName || '')
    setPhone(profile.phone || '')
  }, [profile])

  const roleLabel = STAFF_ROLE_LABELS[profile?.role] || profile?.role || 'Staff'

  async function handleSaveProfile(e) {
    e.preventDefault()
    setSavingProfile(true)
    setError('')
    setMessage('')
    try {
      const data = await updateStaffProfile({
        fullName: fullName.trim(),
        phone: phone.trim() || null,
      })
      setProfile({
        ...profile,
        fullName: data.fullName,
        phone: data.phone || '',
      })
      setMessage('Profile updated.')
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingProfile(false)
    }
  }

  async function handlePhotoChange(e) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploadingPhoto(true)
    setError('')
    setMessage('')
    try {
      const data = await uploadStaffProfilePhoto(file)
      setProfile({
        ...profile,
        profileImageUrl: data.profileImageUrl,
      })
      setMessage('Profile photo updated.')
    } catch (err) {
      setError(err.message)
    } finally {
      setUploadingPhoto(false)
      e.target.value = ''
    }
  }

  async function handleChangePassword(e) {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.')
      return
    }
    setSavingPassword(true)
    setError('')
    setMessage('')
    try {
      await changeStaffPassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setMessage('Password changed.')
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <StaffPage
      title="Profile"
      description="Manage your account details, photo, and appearance preferences."
    >
      <StaffAlert>{error}</StaffAlert>
      {message ? <StaffAlert variant="success">{message}</StaffAlert> : null}

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
        <Card>
          <CardHeader>
            <CardTitle>Photo</CardTitle>
            <CardDescription>Shown in the sidebar and across the staff portal.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col items-center gap-4">
            <div className="relative">
              <Avatar size="lg" className="size-24">
                {profile?.profileImageUrl ? (
                  <AvatarImage src={profile.profileImageUrl} alt={profile.fullName} />
                ) : null}
                <AvatarFallback className="text-lg font-medium">
                  {staffInitials(profile?.fullName)}
                </AvatarFallback>
              </Avatar>
              <Button
                type="button"
                size="icon-sm"
                variant="secondary"
                className="absolute right-0 bottom-0 rounded-full shadow-sm"
                disabled={uploadingPhoto}
                onClick={() => fileInputRef.current?.click()}
              >
                <Camera />
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                className="hidden"
                onChange={handlePhotoChange}
              />
            </div>
            <p className="text-center text-sm text-muted-foreground">
              PNG, JPG, or WebP up to 5 MB.
            </p>
            <Button
              type="button"
              variant="outline"
              disabled={uploadingPhoto}
              onClick={() => fileInputRef.current?.click()}
            >
              {uploadingPhoto ? 'Uploading…' : 'Upload photo'}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Account details</CardTitle>
            <CardDescription>Update how your name appears to other staff.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid gap-4" onSubmit={handleSaveProfile}>
              <div className="grid gap-2">
                <Label htmlFor="profile-full-name">Full name</Label>
                <Input
                  id="profile-full-name"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                  maxLength={120}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="profile-email">Email</Label>
                <Input id="profile-email" value={profile?.email || ''} disabled />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="profile-phone">Phone</Label>
                <Input
                  id="profile-phone"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  maxLength={40}
                  placeholder="Optional"
                />
              </div>
              <div className="grid gap-2">
                <Label>Role</Label>
                <Input value={roleLabel} disabled />
              </div>
              <div className="flex justify-end">
                <Button type="submit" disabled={savingProfile}>
                  {savingProfile ? 'Saving…' : 'Save changes'}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Appearance</CardTitle>
            <CardDescription>
              Your theme preference is saved to your account and restored on every login.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex items-center justify-between gap-4">
            <div className="space-y-1">
              <p className="text-sm font-medium">{isDarkMode ? 'Dark mode' : 'Light mode'}</p>
              <p className="text-sm text-muted-foreground">
                Toggle the staff portal color scheme.
              </p>
            </div>
            <Button
              type="button"
              variant="outline"
              disabled={themeLoading}
              onClick={() => toggleTheme()}
            >
              {isDarkMode ? <Sun /> : <Moon />}
              {isDarkMode ? 'Use light mode' : 'Use dark mode'}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Password</CardTitle>
            <CardDescription>Change your staff portal password.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid gap-4" onSubmit={handleChangePassword}>
              <div className="grid gap-2">
                <Label htmlFor="current-password">Current password</Label>
                <Input
                  id="current-password"
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                  autoComplete="current-password"
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="new-password">New password</Label>
                <Input
                  id="new-password"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="confirm-password">Confirm new password</Label>
                <Input
                  id="confirm-password"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </div>
              <div className="flex justify-end">
                <Button type="submit" disabled={savingPassword}>
                  {savingPassword ? 'Updating…' : 'Update password'}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </StaffPage>
  )
}
