import React from 'react'

interface KokonutCardProps {
  children: React.ReactNode
  className?: string
  glowColor?: string | null
}

export const KokonutCard: React.FC<KokonutCardProps> = ({
  children,
  className = '',
  glowColor,
}) => {
  return (
    <div
      className={`relative overflow-hidden rounded-2xl bg-[#121620] border border-white/[0.08] shadow-lg shadow-black/20 ${className}`}
    >
      {glowColor && (
        <div
          className="pointer-events-none absolute -top-12 -right-12 h-44 w-44 rounded-full blur-3xl opacity-20 transition-all duration-500 ease-out"
          style={{ backgroundColor: glowColor }}
        />
      )}
      <div className="relative z-10 p-4 sm:p-5">{children}</div>
    </div>
  )
}
