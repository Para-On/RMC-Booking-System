import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ScrollReveal from '@/components/motion/ScrollReveal'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { BOOKING_ACTION_BUTTON_CLASS } from '@/lib/bookingFilters'
import { cn } from '@/lib/utils'

export default function LookupPage() {
  const navigate = useNavigate()
  const [reference, setReference] = useState('')
  const [email, setEmail] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    navigate(
      `/booking/${encodeURIComponent(reference.trim())}?email=${encodeURIComponent(email.trim())}`
    )
  }

  return (
    <div className="lookup-page flex w-full justify-center py-2 sm:py-6">
      <ScrollReveal variant="slide-up" trigger="mount" delay={80} className="w-full max-w-md">
        <Card className="border-border/80 shadow-sm">
          <CardHeader className="space-y-2 text-center sm:text-left">
            <CardTitle className="text-xl sm:text-2xl">Find your booking</CardTitle>
            <CardDescription>
              Enter the booking reference and email used at checkout to view or manage your stay.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid gap-4" onSubmit={handleSubmit}>
              <div className="grid gap-2">
                <Label htmlFor="lookup-reference">Booking reference</Label>
                <Input
                  id="lookup-reference"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  placeholder="e.g. RMC-20260714-1234"
                  required
                  autoComplete="off"
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="lookup-email">Email used at booking</Label>
                <Input
                  id="lookup-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  required
                  autoComplete="email"
                />
              </div>
              <Button type="submit" className={cn(BOOKING_ACTION_BUTTON_CLASS, 'w-full')}>
                View booking
              </Button>
            </form>
            <p className="mt-4 text-center text-sm text-muted-foreground sm:text-left">
              Looking for a room?{' '}
              <Link to="/" className="font-medium text-primary underline-offset-4 hover:underline">
                Book a stay
              </Link>
            </p>
          </CardContent>
        </Card>
      </ScrollReveal>
    </div>
  )
}
