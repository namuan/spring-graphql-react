import axios from 'axios'
import type { Configuration, ConfigurationInput, ModelFilter, VehicleModel } from '../types'

const endpoint = import.meta.env.VITE_GRAPHQL_URL || '/graphql'

interface GraphQLResponse<T> {
  data?: T
  errors?: Array<{ message: string }>
}

const client = axios.create({
  baseURL: endpoint,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

async function request<T>(query: string, variables?: Record<string, unknown>): Promise<T> {
  const response = await client.post<GraphQLResponse<T>>('', { query, variables })

  if (response.data.errors?.length) {
    throw new Error(response.data.errors.map((error) => error.message).join('. '))
  }

  if (!response.data.data) {
    throw new Error('The showroom returned an empty response.')
  }

  return response.data.data
}

const MODELS_QUERY = `
  query ShowroomModels($filter: ModelFilter) {
    models(filter: $filter) {
      id
      brand
      name
      modelYear
      basePriceCents
      description
      engine
      powerPs
      accelerationS
      topSpeedKph
      drivetrain
      rangeKm
      seats
      trims { id name priceDeltaCents }
      options { id name category priceCents }
    }
  }
`

const CREATE_CONFIGURATION_MUTATION = `
  mutation CreateConfiguration($modelId: ID!, $trimId: ID!, $optionIds: [ID!]!) {
    createConfiguration(input: {
      modelId: $modelId
      trimId: $trimId
      optionIds: $optionIds
    }) {
      id
      totalPriceCents
      status
      createdAt
      model { name }
      trim { name }
      options { name priceCents }
    }
  }
`

export async function fetchModels(filter?: ModelFilter): Promise<VehicleModel[]> {
  const data = await request<{ models: VehicleModel[] }>(MODELS_QUERY, { filter: filter ?? null })
  return data.models
}

export async function createConfiguration(input: ConfigurationInput): Promise<Configuration> {
  const data = await request<{ createConfiguration: Configuration }>(
    CREATE_CONFIGURATION_MUTATION,
    { modelId: input.modelId, trimId: input.trimId, optionIds: input.optionIds },
  )
  return data.createConfiguration
}
