export interface Trim {
  id: string
  name: string
  priceDeltaCents: number
}

export interface VehicleOption {
  id: string
  name: string
  category: string
  priceCents: number
}

export interface VehicleModel {
  id: string
  brand: string
  name: string
  modelYear: number
  basePriceCents: number
  description: string
  engine: string
  powerPs: number
  accelerationS: number
  topSpeedKph: number
  drivetrain: string
  rangeKm: number | null
  seats: number
  trims: Trim[]
  options: VehicleOption[]
}

export interface Configuration {
  id: string
  totalPriceCents: number
  status: string
  createdAt: string
  model: { name: string }
  trim: { name: string }
  options: Array<{ name: string; priceCents: number }>
}

export interface ConfigurationInput {
  modelId: string
  trimId: string
  optionIds: string[]
}

export interface ModelFilter {
  brand?: string
  minBasePriceCents?: number
  maxBasePriceCents?: number
  minPowerPs?: number
  maxPowerPs?: number
  minTopSpeedKph?: number
  maxTopSpeedKph?: number
  minAccelerationS?: number
  maxAccelerationS?: number
  minSeats?: number
  maxSeats?: number
}
