import { AppState } from './types'

type StateListener = (state: AppState) => void

let currentState: AppState = {
  connected: false,
  connecting: false,
  scanning: false,
  device_name: '',
  saved_device_name: null,
  saved_device_address: null,
  status_text: 'Ready to connect',
  brightness: 100,
  devices: [],
  applied_color: [255, 142, 94],
  base_color: [255, 142, 94],
}

const listeners = new Set<StateListener>()
let hasBackend = false

function emitState(newState: Partial<AppState>) {
  currentState = { ...currentState, ...newState }
  listeners.forEach((listener) => listener(currentState))
}

// Global callback hook
declare global {
  interface Window {
    __onLumoraState?: (state: AppState) => void
  }
}

if (typeof window !== 'undefined') {
  window.__onLumoraState = (state: AppState) => {
    hasBackend = true
    emitState(state)
  }

  // Connect to SSE stream
  function connectEventSource() {
    try {
      const es = new EventSource('/api/events')
      es.onopen = () => {
        hasBackend = true
      }
      es.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          hasBackend = true
          emitState(data)
        } catch {
          // Ignored
        }
      }
      es.onerror = () => {
        es.close()
        setTimeout(connectEventSource, 2500)
      }
    } catch {
      // Standalone mode
    }
  }

  connectEventSource()
}

async function postApi(endpoint: string, body: Record<string, unknown> = {}) {
  try {
    const res = await fetch(`/api/${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    return res.ok
  } catch {
    return false
  }
}

export const bridge = {
  subscribe(listener: StateListener): () => void {
    listeners.add(listener)
    listener(currentState)
    return () => listeners.delete(listener)
  },

  getState(): AppState {
    return currentState
  },

  scan() {
    if (hasBackend) {
      postApi('scan')
      return
    }
    // Dev mock
    emitState({ scanning: true, status_text: 'Scanning for BLE devices...' })
    setTimeout(() => {
      emitState({
        scanning: false,
        status_text: 'Found 2 devices',
        devices: [
          { name: 'Triones-7719', address: 'AA:BB:CC:DD:EE:01', display_name: 'Triones-7719' },
          { name: 'LEDnet-WF', address: 'AA:BB:CC:DD:EE:02', display_name: 'LEDnet-WF' },
        ],
      })
    }, 1200)
  },

  connect(address: string) {
    if (hasBackend) {
      postApi('connect', { address })
      return
    }
    // Dev mock
    emitState({ connecting: true, status_text: `Connecting to ${address}...` })
    setTimeout(() => {
      emitState({
        connecting: false,
        connected: true,
        device_name: 'Triones-7719',
        saved_device_name: 'Triones-7719',
        saved_device_address: address,
        status_text: 'Connected to Triones-7719',
      })
    }, 1000)
  },

  disconnect() {
    if (hasBackend) {
      postApi('disconnect')
      return
    }
    emitState({
      connected: false,
      connecting: false,
      status_text: 'Disconnected',
    })
  },

  resetConnection() {
    if (hasBackend) {
      postApi('reset')
      return
    }
    this.scan()
  },

  setColor(r: number, g: number, b: number) {
    emitState({ applied_color: [r, g, b], base_color: [r, g, b] })
    if (hasBackend) {
      postApi('color', { r, g, b })
    }
  },

  setBrightness(level: number) {
    emitState({ brightness: level })
    if (hasBackend) {
      postApi('brightness', { brightness: level })
    }
  },

  turnOn() {
    if (hasBackend) {
      postApi('turn_on')
    }
  },

  turnOff() {
    if (hasBackend) {
      postApi('turn_off')
    }
  },
}
