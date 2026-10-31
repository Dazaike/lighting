export interface LightDevice {
  name: string
  address: string
  display_name: string
  rssi?: number
}

export interface AppState {
  connected: boolean
  connecting: boolean
  scanning: boolean
  device_name: string
  saved_device_name: string | null
  saved_device_address: string | null
  status_text: string
  brightness: number
  devices: LightDevice[]
  applied_color: [number, number, number]
  base_color: [number, number, number]
}
