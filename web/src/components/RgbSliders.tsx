import React from 'react'

interface RgbSlidersProps {
  color: [number, number, number]
  onChange: (rgb: [number, number, number]) => void
  className?: string
}

export const RgbSliders: React.FC<RgbSlidersProps> = ({ color, onChange, className = '' }) => {
  const [r, g, b] = color

  const updateChannel = (idx: 0 | 1 | 2, val: number) => {
    const next: [number, number, number] = [...color]
    next[idx] = val
    onChange(next)
  }

  const channels: { label: string; value: number; colorClass: string; idx: 0 | 1 | 2 }[] = [
    { label: 'R', value: r, colorClass: 'text-[#ff6b6b]', idx: 0 },
    { label: 'G', value: g, colorClass: 'text-[#56d364]', idx: 1 },
    { label: 'B', value: b, colorClass: 'text-[#5da9ff]', idx: 2 },
  ]

  return (
    <div className={`flex flex-col gap-3 py-1 ${className}`}>
      {channels.map(({ label, value, colorClass, idx }) => (
        <div key={label} className="flex items-center gap-3">
          <span className={`w-4 text-xs font-bold ${colorClass}`}>{label}</span>
          <input
            type="range"
            min={0}
            max={255}
            value={value}
            onChange={(e) => updateChannel(idx, Number(e.target.value))}
            className="flex-1"
          />
          <span className="w-8 text-right font-mono text-xs font-semibold text-slate-400">
            {value}
          </span>
        </div>
      ))}
    </div>
  )
}
