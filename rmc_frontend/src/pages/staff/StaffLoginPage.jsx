import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { BrandMark } from '@/components/branding/BrandMark'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { StaffAlert } from '@/components/staff/StaffPageShell'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { staffLogin, verifyStaffMfa } from '@/staffApi'

export default function StaffLoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('frontdesk@rmc.local')
  const [password, setPassword] = useState('')
  const [mfaToken, setMfaToken] = useState('')
  const [mfaCode, setMfaCode] = useState('')
  const [mfaStep, setMfaStep] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const result = await staffLogin(email, password)
      if (result.mfaRequired) {
        setMfaToken(result.mfaToken)
        setMfaStep(true)
      } else {
        navigate('/staff/dashboard')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleMfaSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await verifyStaffMfa(mfaToken, mfaCode)
      navigate('/staff/arrivals')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="lookup-page flex w-full justify-center py-2 sm:py-6">
      <ScrollReveal variant="slide-up" trigger="mount" delay={80} className="w-full max-w-md">
        <Card className="border-border/80 shadow-sm">
          <CardHeader className="space-y-3 text-center">
            <div className="flex justify-center">
              <BrandMark />
            </div>
            <CardTitle className="text-xl sm:text-2xl">Staff login</CardTitle>
            <CardDescription>Sign in to the staff portal</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {error ? <StaffAlert>{error}</StaffAlert> : null}

            {!mfaStep ? (
              <form className="grid gap-4" onSubmit={handleSubmit}>
                <div className="grid gap-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoComplete="username"
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="password">Password</Label>
                  <Input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    autoComplete="current-password"
                  />
                </div>
                <Button type="submit" className="w-full" disabled={loading}>
                  {loading ? 'Signing in…' : 'Sign in'}
                </Button>
              </form>
            ) : (
              <form className="grid gap-4" onSubmit={handleMfaSubmit}>
                <p className="text-sm text-muted-foreground">
                  Enter the 6-digit code from your authenticator app.
                </p>
                <div className="grid gap-2">
                  <Label htmlFor="mfa-code">Authentication code</Label>
                  <Input
                    id="mfa-code"
                    value={mfaCode}
                    onChange={(e) => setMfaCode(e.target.value)}
                    required
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                  />
                </div>
                <Button type="submit" className="w-full" disabled={loading}>
                  {loading ? 'Verifying…' : 'Verify'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  className="w-full"
                  onClick={() => {
                    setMfaStep(false)
                    setMfaCode('')
                    setMfaToken('')
                  }}
                >
                  Back
                </Button>
              </form>
            )}

            <p className="text-center text-sm text-muted-foreground">
              <Link to="/" className="font-medium text-primary underline-offset-4 hover:underline">
                ← Back to booking site
              </Link>
            </p>

            <p className="text-xs text-muted-foreground">
              Dev accounts: <code className="rounded bg-muted px-1">frontdesk@rmc.local</code> /{' '}
              <code className="rounded bg-muted px-1">staff123</code>
              {' · '}
              <code className="rounded bg-muted px-1">admin@rmc.local</code> /{' '}
              <code className="rounded bg-muted px-1">manager123</code>
            </p>
          </CardContent>
        </Card>
      </ScrollReveal>
    </div>
  )
}
