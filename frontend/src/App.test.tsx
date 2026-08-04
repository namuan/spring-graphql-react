import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import * as api from './api/graphql'

vi.mock('./api/graphql')

const models = [{
  id: 'model-1',
  brand: 'Aureline',
  name: 'Aster Vale',
  modelYear: 2026,
  basePriceCents: 8_000_000,
  description: 'A grand tourer shaped for long horizons.',
  engine: 'Twin-turbo V6',
  powerPs: 520,
  accelerationS: 3.9,
  topSpeedKph: 300,
  drivetrain: 'AWD',
  rangeKm: null,
  seats: 4,
  trims: [{ id: 'trim-1', name: 'Apex', priceDeltaCents: 500_000 }],
  options: [{ id: 'option-1', name: 'Panoramic canopy', category: 'Light & atmosphere', priceCents: 250_000 }],
}]

function renderApp() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><App /></QueryClientProvider>)
}

describe('vehicle configurator', () => {
  beforeEach(() => {
    vi.mocked(api.fetchModels).mockResolvedValue(models)
    vi.mocked(api.createConfiguration).mockResolvedValue({
      id: 'CFG-2026-001',
      totalPriceCents: 8_750_000,
      status: 'SAVED',
      createdAt: '2026-08-04T10:30:00Z',
      model: { name: 'Aster Vale' },
      trim: { name: 'Apex' },
      options: [{ name: 'Panoramic canopy', priceCents: 250_000 }],
    })
  })

  it('selects a complete specification and saves it', async () => {
    const user = userEvent.setup()
    renderApp()

    // Stage 1: the model is presented as a card with its tech specification.
    const modelCard = await screen.findByRole('button', { name: /Configure Aster Vale/i })
    expect(screen.getByText('520 PS')).toBeInTheDocument()
    expect(screen.getByText('AWD')).toBeInTheDocument()
    await user.click(modelCard)

    // Stage 2: trim, option, then save.
    await user.click(screen.getByRole('radio', { name: /Apex/i }))
    await user.click(screen.getByRole('checkbox', { name: /Panoramic canopy/i }))
    await user.click(screen.getByRole('button', { name: /Save configuration/i }))

    expect(await screen.findByRole('heading', { name: 'Configuration saved' })).toBeInTheDocument()
    expect(screen.getByText(/CFG-2026-001/)).toBeInTheDocument()
    expect(vi.mocked(api.createConfiguration).mock.calls[0][0]).toEqual({
      modelId: 'model-1',
      trimId: 'trim-1',
      optionIds: ['option-1'],
    })
  })

  it('shows the empty showroom state', async () => {
    vi.mocked(api.fetchModels).mockResolvedValue([])
    renderApp()
    expect(await screen.findByRole('heading', { name: /No models are on the floor today/i })).toBeInTheDocument()
  })

  it('filters the gallery by brand through the GraphQL query', async () => {
    const user = userEvent.setup()
    renderApp()
    await screen.findByRole('button', { name: /Configure Aster Vale/i })

    await user.selectOptions(screen.getByRole('combobox', { name: /Filter by brand/i }), 'Aureline')

    await waitFor(() => {
      expect(vi.mocked(api.fetchModels)).toHaveBeenCalledWith(
        expect.objectContaining({ brand: 'Aureline' }),
      )
    })
  })
})
