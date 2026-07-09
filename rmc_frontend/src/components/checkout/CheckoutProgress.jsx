import RoomTypeWizardStepper from '@/components/staff/RoomTypeWizardStepper'

const STEPS = [
  { id: 'room', label: 'Room' },
  { id: 'extras', label: 'Extras' },
  { id: 'guest', label: 'Guest' },
  { id: 'payment', label: 'Payment' },
  { id: 'confirm', label: 'Confirm' },
]

export function CheckoutProgress({ currentStep }) {
  return <RoomTypeWizardStepper steps={STEPS} currentStep={currentStep} />
}

export { STEPS }
