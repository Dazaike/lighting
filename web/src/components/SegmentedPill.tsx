import React from 'react'
import { motion } from 'framer-motion'

interface SegmentedPillOption<T extends string> {
  id: T
  label: string
}

interface SegmentedPillProps<T extends string> {
  options: SegmentedPillOption<T>[]
  selected: T
  onChange: (id: T) => void
  className?: string
}

export function SegmentedPill<T extends string>({
  options,
  selected,
  onChange,
  className = '',
}: SegmentedPillProps<T>) {
  return (
    <div
      className={`relative flex items-center rounded-full bg-[#0d121c] p-1 border border-white/[0.06] ${className}`}
    >
      {options.map((option) => {
        const isSelected = selected === option.id
        return (
          <motion.button
            key={option.id}
            onClick={() => onChange(option.id)}
            whileTap={{ scale: 0.96 }}
            className={`relative z-10 flex-1 rounded-full px-4 py-1.5 text-xs font-bold transition-colors cursor-pointer select-none ${
              isSelected ? 'text-[#0c0704]' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            {isSelected && (
              <motion.div
                layoutId="segmented-pill-active"
                className="absolute inset-0 z-[-1] rounded-full bg-[#ff8e5e] shadow-md shadow-[#ff8e5e]/25"
                transition={{
                  type: 'spring',
                  stiffness: 380,
                  damping: 28,
                }}
              />
            )}
            {option.label}
          </motion.button>
        )
      })}
    </div>
  )
}
