import React, { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Power, RefreshCw, Bluetooth, Radio, Wrench } from 'lucide-react'
import { bridge } from './bridge'
import { AppState } from './types'
import { KokonutCard } from './components/KokonutCard'
import { PulsingBadge } from './components/PulsingBadge'
import { SegmentedPill } from './components/SegmentedPill'
import { ColorWheel } from './components/ColorWheel'
import { RgbSliders } from './components/RgbSliders'

export default function App() {
  const [state, setState] = useState<AppState>(bridge.getState())
  const [selectedAddress, setSelectedAddress] = useState<string>('')
  const [pickerMode, setPickerMode] = useState<'wheel' | 'rgb'>('wheel')

  useEffect(() => {
    return bridge.subscribe((newState) => {
      setState(newState)
      if (newState.saved_device_address && !selectedAddress) {
        setSelectedAddress(newState.saved_device_address)
      } else if (newState.devices.length > 0 && !selectedAddress) {
        setSelectedAddress(newState.devices[0].address)
      }
    })
  }, [selectedAddress])

  const [r, g, b] = state.applied_color || [255, 142, 94]
  const hexColor = `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b
    .toString(16)
    .padStart(2, '0')}`
  const luma = 0.299 * r + 0.587 * g + 0.114 * b
  const textColor = luma > 140 ? '#0c0704' : '#dde3ec'

  const busy = state.scanning || state.connecting
  const canTarget = state.devices.length > 0 || Boolean(state.saved_device_address)

  // Status pill config
  let statusText = 'Idle'
  let statusColor = '#64748b'
  let isPulsing = false

  if (state.scanning) {
    statusText = 'Scanning'
    statusColor = '#ff8e5e'
    isPulsing = true
  } else if (state.connecting) {
    statusText = 'Connecting'
    statusColor = '#ffc766'
    isPulsing = true
  } else if (state.connected) {
    statusText = 'Connected'
    statusColor = '#4de3a0'
    isPulsing = true
  }

  const handleColorChange = (rgb: [number, number, number]) => {
    bridge.setColor(rgb[0], rgb[1], rgb[2])
  }

  const handleBrightnessChange = (val: number) => {
    bridge.setBrightness(val)
  }

  return (
    <div className="mx-auto flex min-h-screen w-full max-w-[440px] flex-col gap-3 p-3.5 sm:p-4 selection:bg-[#ff8e5e]/30 selection:text-white">
      {/* ── 1. Header Bento Card ── */}
      <KokonutCard glowColor={state.connected ? hexColor : null}>
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <div className="flex flex-col gap-0.5">
              <div className="flex items-center gap-2">
                <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/[0.06] p-1 border border-white/[0.08]">
                  <img src="./icon.png" alt="Lumora" className="h-5 w-5 object-contain" />
                </div>
                <h1 className="text-xl font-bold tracking-tight text-white">Lumora</h1>
                <motion.button
                  whileTap={{ scale: 0.94 }}
                  onClick={() => bridge.scan()}
                  disabled={busy}
                  className="ml-1 inline-flex items-center gap-1 rounded-full bg-white/[0.07] px-2.5 py-0.5 text-xs font-semibold text-slate-300 hover:bg-white/[0.12] hover:text-white disabled:opacity-40 transition-colors cursor-pointer"
                >
                  <RefreshCw className={`h-3 w-3 ${state.scanning ? 'animate-spin' : ''}`} />
                  <span>Resync</span>
                </motion.button>
              </div>
              <p className="text-xs text-slate-400">
                {state.connected
                  ? state.device_name || 'Connected device'
                  : state.scanning
                  ? 'Searching for your controller'
                  : state.saved_device_name
                  ? `Saved: ${state.saved_device_name}`
                  : 'Ready to connect'}
              </p>
            </div>
            <PulsingBadge text={statusText} color={statusColor} pulsing={isPulsing} />
          </div>

          {/* Animated level progress bar */}
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-800/80">
            <motion.div
              className="h-full rounded-full bg-gradient-to-r from-[#ff8e5e] via-[#57e0c3] to-[#6b8cff]"
              animate={{
                width: state.connected
                  ? '100%'
                  : state.connecting
                  ? '75%'
                  : state.scanning
                  ? '45%'
                  : '15%',
              }}
              transition={{ type: 'spring', stiffness: 220, damping: 24 }}
            />
          </div>
        </div>
      </KokonutCard>

      {/* ── 2. Device Bento Card ── */}
      <KokonutCard>
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-[2px] text-slate-400">
              DEVICE
            </span>
            {state.saved_device_name && (
              <span className="text-[11px] text-slate-400 truncate max-w-[200px]">
                Saved: {state.saved_device_name}
              </span>
            )}
          </div>

          <div className="flex gap-2">
            <div className="relative flex-1">
              <select
                value={selectedAddress}
                onChange={(e) => setSelectedAddress(e.target.value)}
                disabled={busy}
                className="w-full appearance-none rounded-xl border border-white/[0.08] bg-[#161c28] px-3 py-2 text-xs font-medium text-slate-200 hover:border-white/[0.18] focus:border-[#ff8e5e] focus:outline-none transition-colors cursor-pointer disabled:opacity-40"
              >
                {state.devices.length === 0 ? (
                  <option value="">
                    {state.saved_device_address
                      ? `[Saved] ${state.saved_device_name || state.saved_device_address}`
                      : 'No devices found (click Scan)'}
                  </option>
                ) : (
                  state.devices.map((d) => (
                    <option key={d.address} value={d.address}>
                      [{d.name.toLowerCase().includes('triones') || d.name.toLowerCase().includes('led') ? 'HL' : 'BLE'}]{' '}
                      {d.display_name || d.name || d.address}
                    </option>
                  ))
                )}
              </select>
              <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                <Bluetooth className="h-3.5 w-3.5" />
              </div>
            </div>

            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => bridge.scan()}
              disabled={busy}
              className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-white/[0.08] bg-[#161c28] px-3.5 py-2 text-xs font-bold text-slate-200 hover:bg-[#1e2638] hover:text-white disabled:opacity-40 transition-colors cursor-pointer"
            >
              <Radio className={`h-3.5 w-3.5 ${state.scanning ? 'animate-pulse text-[#ff8e5e]' : ''}`} />
              <span>Scan</span>
            </motion.button>
          </div>

          <div className="flex gap-2">
            <motion.button
              whileTap={{ scale: 0.96 }}
              onClick={() => bridge.connect(selectedAddress || state.saved_device_address || '')}
              disabled={busy || !canTarget || state.connected}
              className="flex-1 rounded-xl bg-gradient-to-r from-[#ff8e5e] to-[#ff7a40] py-2 text-xs font-bold text-[#0c0704] shadow-md shadow-[#ff8e5e]/20 hover:from-[#ffa073] hover:to-[#ff8a52] disabled:opacity-40 disabled:pointer-events-none transition-all cursor-pointer"
            >
              {state.connecting ? 'Connecting...' : 'Connect'}
            </motion.button>

            <motion.button
              whileTap={{ scale: 0.96 }}
              onClick={() => bridge.disconnect()}
              disabled={!state.connected}
              className="flex-1 rounded-xl border border-white/[0.08] bg-[#161c28] py-2 text-xs font-semibold text-slate-300 hover:bg-[#1e2638] hover:text-white disabled:opacity-40 disabled:pointer-events-none transition-colors cursor-pointer"
            >
              Disconnect
            </motion.button>

            <motion.button
              whileTap={{ scale: 0.96 }}
              onClick={() => bridge.resetConnection()}
              disabled={busy || !state.saved_device_address}
              title="Fix connection & reconnect"
              className="rounded-xl border border-red-500/25 bg-red-500/10 px-3 py-2 text-xs font-semibold text-red-400 hover:bg-red-500/20 hover:text-red-300 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
            >
              <Wrench className="h-3.5 w-3.5" />
            </motion.button>
          </div>
        </div>
      </KokonutCard>

      {/* ── 3. Color & Light Bento Card ── */}
      <KokonutCard glowColor={hexColor}>
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-[2px] text-slate-400">
              COLOR
            </span>
            <SegmentedPill
              options={[
                { id: 'wheel', label: 'Wheel' },
                { id: 'rgb', label: 'RGB' },
              ]}
              selected={pickerMode}
              onChange={setPickerMode}
            />
          </div>

          <AnimatePresence mode="wait">
            {pickerMode === 'wheel' ? (
              <motion.div
                key="wheel"
                initial={{ opacity: 0, scale: 0.97 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.97 }}
                transition={{ duration: 0.18 }}
              >
                <ColorWheel color={[r, g, b]} onChange={handleColorChange} />
              </motion.div>
            ) : (
              <motion.div
                key="rgb"
                initial={{ opacity: 0, scale: 0.97 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.97 }}
                transition={{ duration: 0.18 }}
                className="py-3"
              >
                <RgbSliders color={[r, g, b]} onChange={handleColorChange} />
              </motion.div>
            )}
          </AnimatePresence>

          {/* Color preview bar */}
          <div
            className="flex h-8 w-full items-center justify-center rounded-xl border border-white/[0.12] text-xs font-bold transition-colors shadow-inner"
            style={{
              backgroundColor: hexColor,
              color: textColor,
            }}
          >
            <span>{hexColor.toUpperCase()}</span>
          </div>

          {/* Brightness row */}
          <div className="flex flex-col gap-1.5 pt-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold uppercase tracking-[2px] text-slate-400">
                BRIGHTNESS
              </span>
              <span className="font-mono text-xs font-semibold text-slate-300">
                {state.brightness}%
              </span>
            </div>
            <input
              type="range"
              min={1}
              max={100}
              value={state.brightness}
              onChange={(e) => handleBrightnessChange(Number(e.target.value))}
              className="w-full"
            />
          </div>

          {/* Power Controls */}
          <div className="flex gap-2.5 pt-2">
            <motion.button
              whileTap={{ scale: 0.96 }}
              onClick={() => bridge.turnOn()}
              disabled={!state.connected}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-full bg-gradient-to-r from-[#ff8e5e] to-[#ff7a40] py-2.5 text-xs font-bold text-[#0c0704] shadow-lg shadow-[#ff8e5e]/25 hover:from-[#ffa073] hover:to-[#ff8a52] disabled:opacity-40 disabled:pointer-events-none transition-all cursor-pointer"
            >
              <Power className="h-3.5 w-3.5" />
              <span>Power On</span>
            </motion.button>

            <motion.button
              whileTap={{ scale: 0.96 }}
              onClick={() => bridge.turnOff()}
              disabled={!state.connected}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 py-2.5 text-xs font-bold text-red-400 hover:bg-red-500/20 hover:text-red-300 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
            >
              <Power className="h-3.5 w-3.5" />
              <span>Power Off</span>
            </motion.button>
          </div>
        </div>
      </KokonutCard>

      {/* ── 4. Status Bento Card ── */}
      <KokonutCard>
        <div className="flex items-center justify-between text-xs text-slate-400">
          <span className="truncate">{state.status_text}</span>
          <span className="font-mono text-[10px] opacity-60">Lumora v0.4.0</span>
        </div>
      </KokonutCard>
    </div>
  )
}
