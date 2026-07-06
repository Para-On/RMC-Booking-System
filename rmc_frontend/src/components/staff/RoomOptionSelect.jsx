import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

export default function RoomOptionSelect({
  label,
  options,
  value,
  onChange,
  required = true,
}) {
  return (
    <div className="grid gap-2">
      <Label>{label}</Label>
      <Select value={value || ''} onValueChange={onChange} required={required}>
        <SelectTrigger>
          <SelectValue placeholder={`Select ${label.toLowerCase()}`} />
        </SelectTrigger>
        <SelectContent>
          {options.map((opt) => (
            <SelectItem key={opt.id} value={String(opt.id)}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
