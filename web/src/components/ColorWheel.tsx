import React, { useRef, useEffect, useState, useCallback } from 'react'
import { motion } from 'framer-motion'

interface ColorWheelProps {
  color: [number, number, number]
  onChange: (rgb: [number, number, number]) => void
  className?: string
}

function rgbToHsv(r: number, g: number, b: number): [number, number, number] {
  const nr = r / 255
  const ng = g / 255
  const nb = b / 255
  const max = Math.max(nr, ng, nb)
  const min = Math.min(nr, ng, nb)
  const d = max - min
  let h = 0
  const s = max === 0 ? 0 : d / max
  const v = max

  if (max !== min) {
    switch (max) {
      case nr:
        h = (ng - nb) / d + (ng < nb ? 6 : 0)
        break
      case ng:
        h = (nb - nr) / d + 2
        break
      case nb:
        h = (nr - ng) / d + 4
        break
    }
    h /= 6
  }
  return [h * 360, s, v]
}

function hsvToRgb(h: number, s: number, v: number): [number, number, number] {
  const c = v * s
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = v - c
  let r = 0,
    g = 0,
    b = 0

  if (h >= 0 && h < 60) {
    r = c
    g = x
  } else if (h >= 60 && h < 120) {
    r = x
    g = c
  } else if (h >= 120 && h < 180) {
    g = c
    b = x
  } else if (h >= 180 && h < 240) {
    g = x
    b = c
  } else if (h >= 240 && h < 300) {
    r = x
    b = c
  } else if (h >= 300 && h < 360) {
    r = c
    b = x
  }

  return [Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255)]
}

export const ColorWheel: React.FC<ColorWheelProps> = ({ color, onChange, className = '' }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const containerRef = useRef<HTMLDivElement | null>(null)
  const [isDragging, setIsDragging] = useState(false)

  const size = 230
  const radius = size / 2 - 8
  const center = size / 2

  const [h, s] = rgbToHsv(...color)
  const hexColor = `#${color.map((c) => c.toString(16).padStart(2, '0')).join('')}`

  // Render the color wheel canvas once
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    ctx.clearRect(0, 0, size, size)

    // Hue conical gradient
    const gradient = ctx.createConicGradient(0, center, center)
    const stops = [
      '#ff0000',
      '#ffff00',
      '#00ff00',
      '#00ffff',
      '#0000ff',
      '#ff00ff',
      '#ff0000',
    ]
    stops.forEach((c, idx) => gradient.addColorStop(idx / (stops.length - 1), c))

    ctx.beginPath()
    ctx.arc(center, center, radius, 0, Math.PI * 2)
    ctx.fillStyle = gradient
    ctx.fill()

    // White center radial gradient for saturation
    const radial = ctx.createRadialGradient(center, center, 0, center, center, radius)
    radial.addColorStop(0, 'rgba(255, 255, 255, 1)')
    radial.addColorStop(1, 'rgba(255, 255, 255, 0)')

    ctx.beginPath()
    ctx.arc(center, center, radius, 0, Math.PI * 2)
    ctx.fillStyle = radial
    ctx.fill()

    // Subtle frosted outer border
    ctx.beginPath()
    ctx.arc(center, center, radius, 0, Math.PI * 2)
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)'
    ctx.lineWidth = 1.5
    ctx.stroke()
  }, [center, radius, size])

  // Handle position based on Hue and Saturation
  const angleRad = (h * Math.PI) / 180
  const handleDist = s * radius
  const handleX = center + Math.cos(angleRad) * handleDist
  const handleY = center + Math.sin(angleRad) * handleDist

  const handlePointer = useCallback(
    (clientX: number, clientY: number) => {
      const container = containerRef.current
      if (!container) return
      const rect = container.getBoundingClientRect()
      const x = clientX - rect.left - center
      const y = clientY - rect.top - center

      const dist = Math.min(Math.hypot(x, y), radius)
      let angle = (Math.atan2(y, x) * 180) / Math.PI
      if (angle < 0) angle += 360

      const newSat = dist / radius
      const newRgb = hsvToRgb(angle, newSat, 1.0)
      onChange(newRgb)
    },
    [center, onChange, radius]
  )

  const onPointerDown = (e: React.PointerEvent) => {
    setIsDragging(true)
    handlePointer(e.clientX, e.clientY)
    ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
  }

  const onPointerMove = (e: React.PointerEvent) => {
    if (isDragging) {
      handlePointer(e.clientX, e.clientY)
    }
  }

  const onPointerUp = (e: React.PointerEvent) => {
    setIsDragging(false)
    try {
      ;(e.target as HTMLElement).releasePointerCapture(e.pointerId)
    } catch {
      // Ignored
    }
  }

  return (
    <div
      ref={containerRef}
      className={`relative flex items-center justify-center p-3 select-none touch-none ${className}`}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerUp}
    >
      {/* Multi-layer atmospheric blur backdrop */}
      <div
        className="pointer-events-none absolute h-[240px] w-[240px] rounded-full blur-2xl opacity-35 transition-all duration-300 ease-out"
        style={{ backgroundColor: hexColor }}
      />
      <div
        className="pointer-events-none absolute h-[200px] w-[200px] rounded-full blur-xl opacity-45 transition-all duration-300 ease-out"
        style={{ backgroundColor: hexColor }}
      />

      <canvas
        ref={canvasRef}
        width={size}
        height={size}
        className="relative z-10 cursor-crosshair rounded-full shadow-2xl"
      />

      {/* Dual-ring frosted lens thumb with Motion spring physics */}
      <motion.div
        className="pointer-events-none absolute z-20 h-6 w-6 -ml-3 -mt-3 rounded-full flex items-center justify-center shadow-lg"
        style={{
          left: handleX,
          top: handleY,
        }}
        animate={{
          scale: isDragging ? 1.25 : 1,
        }}
        transition={{
          type: 'spring',
          stiffness: 400,
          damping: 24,
        }}
      >
        <div className="h-6 w-6 rounded-full border-2 border-white/90 shadow-sm flex items-center justify-center backdrop-blur-sm bg-black/20">
          <div
            className="h-3 w-3 rounded-full border border-black/30 shadow-inner"
            style={{ backgroundColor: hexColor }}
          />
        </div>
      </motion.div>
    </div>
  )
}
