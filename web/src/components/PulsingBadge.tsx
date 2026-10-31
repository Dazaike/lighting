import React from 'react'
import { motion } from 'framer-motion'

interface PulsingBadgeProps {
  text: string
  color: string
  pulsing?: boolean
  className?: string
}

export const PulsingBadge: React.FC<PulsingBadgeProps> = ({
  text,
  color,
  pulsing = true,
  className = '',
}) => {
  return (
    <div
      className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold tracking-wide border transition-all ${className}`}
      style={{
        backgroundColor: `${color}1a`, // 10% opacity
        borderColor: `${color}40`,     // 25% opacity
        color: color,
      }}
    >
      <div className="relative flex h-2 w-2 items-center justify-center">
        {pulsing && (
          <motion.span
            className="absolute h-full w-full rounded-full opacity-75"
            style={{ backgroundColor: color }}
            animate={{
              scale: [1, 1.85, 1],
              opacity: [0.75, 0, 0.75],
            }}
            transition={{
              duration: 1.6,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
        )}
        <span
          className="relative inline-flex h-1.5 w-1.5 rounded-full"
          style={{ backgroundColor: color }}
        />
      </div>
      <span>{text}</span>
    </div>
  )
}
