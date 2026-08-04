import { useEffect, useMemo, useState } from 'react'
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { ArrowDown, ArrowRight, Check, ChevronLeft, RotateCcw } from 'lucide-react'
import { createConfiguration, fetchModels } from './api/graphql'
import { formatDate, formatGBP } from './lib/format'
import type { Configuration, ModelFilter, VehicleModel } from './types'

function useDebouncedValue<T>(value: T, delay = 250): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}

function extent(values: number[]): [number, number] {
  if (values.length === 0) return [0, 1]
  const min = Math.min(...values)
  const max = Math.max(...values)
  return min === max ? [min, max + 1] : [min, max]
}

interface FilterBounds {
  price: [number, number]
  power: [number, number]
  speed: [number, number]
  accel: [number, number]
  seats: [number, number]
}

function computeBounds(models: VehicleModel[]): FilterBounds {
  return {
    price: extent(models.map((m) => m.basePriceCents)),
    power: extent(models.map((m) => m.powerPs)),
    speed: extent(models.map((m) => m.topSpeedKph)),
    accel: extent(models.map((m) => m.accelerationS)),
    seats: extent(models.map((m) => m.seats)),
  }
}

function Header() {
  return (
    <header className="site-header">
      <a className="wordmark" href="#top" aria-label="Aureline Motor House, home">
        <span className="wordmark-mark" aria-hidden="true">A</span>
        <span>Aureline <small>Motor House</small></span>
      </a>
      <a className="header-link" href="#collection">
        The collection <ArrowDown aria-hidden="true" size={14} />
      </a>
    </header>
  )
}

function Hero() {
  return (
    <section className="hero" id="top" aria-labelledby="hero-title">
      <div className="hero-kicker">The 2026 collection · Crafted in Britain</div>
      <div className="hero-title-wrap">
        <h1 id="hero-title">Motion,<br /><em>made personal.</em></h1>
        <p>Choose the form. Define the character. We will take care of everything between the first line and the open road.</p>
      </div>
      <div className="hero-art" aria-hidden="true">
        <span className="hero-year">26</span>
        <svg viewBox="0 0 760 230" role="presentation">
          <path d="M36 177 C128 167, 161 123, 236 99 C335 67, 493 72, 566 104 C613 125, 643 157, 725 170" />
          <path d="M205 112 C284 107, 404 103, 542 113" />
          <circle cx="195" cy="174" r="35" />
          <circle cx="596" cy="174" r="35" />
        </svg>
        <span className="hero-caption">A study in considered movement</span>
      </div>
    </section>
  )
}

function LoadingState() {
  return (
    <section className="state-panel" aria-live="polite" aria-busy="true">
      <span className="loading-mark" aria-hidden="true" />
      <p className="eyebrow">Opening the showroom</p>
      <h2>Preparing the collection.</h2>
    </section>
  )
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <section className="state-panel" role="alert">
      <p className="eyebrow">Connection interrupted</p>
      <h2>The showroom is temporarily out of reach.</h2>
      <p>Your selections have not been changed. Please try the connection again.</p>
      <button className="secondary-button" type="button" onClick={onRetry}>
        Try again <RotateCcw size={15} aria-hidden="true" />
      </button>
    </section>
  )
}

function EmptyState() {
  return (
    <section className="state-panel">
      <p className="eyebrow">Between collections</p>
      <h2>No models are on the floor today.</h2>
      <p>Our next collection is being prepared. Please return soon.</p>
    </section>
  )
}

function StepHeading({ number, title, note }: { number: string; title: string; note: string }) {
  return (
    <div className="step-heading">
      <span>{number}</span>
      <div>
        <h2>{title}</h2>
        <p>{note}</p>
      </div>
    </div>
  )
}

/* ---------- Stage 1 filters ---------- */

interface RangeFilterProps {
  label: string
  min: number
  max: number
  step: number
  valueMin: number | undefined
  valueMax: number | undefined
  format: (value: number) => string
  onChange: (min: number, max: number) => void
}

function RangeFilter({ label, min, max, step, valueMin, valueMax, format, onChange }: RangeFilterProps) {
  const currentMin = valueMin ?? min
  const currentMax = valueMax ?? max
  return (
    <div className="range-filter">
      <div className="range-filter-head">
        <span className="filter-label">{label}</span>
        <span className="range-readout">{format(currentMin)} — {format(currentMax)}</span>
      </div>
      <div className="range-track">
        <input
          type="range"
          aria-label={`Minimum ${label}`}
          min={min}
          max={max}
          step={step}
          value={currentMin}
          onChange={(event) => onChange(Math.min(Number(event.target.value), currentMax), currentMax)}
        />
        <input
          type="range"
          aria-label={`Maximum ${label}`}
          min={min}
          max={max}
          step={step}
          value={currentMax}
          onChange={(event) => onChange(currentMin, Math.max(Number(event.target.value), currentMin))}
        />
      </div>
    </div>
  )
}

interface ModelFiltersProps {
  brands: string[]
  bounds: FilterBounds
  filters: ModelFilter
  count: number
  isUpdating: boolean
  onBrandChange: (brand: string | undefined) => void
  onPriceChange: (min: number, max: number) => void
  onPowerChange: (min: number, max: number) => void
  onSpeedChange: (min: number, max: number) => void
  onAccelChange: (min: number, max: number) => void
  onSeatsChange: (min: number, max: number) => void
  onClear: () => void
}

function ModelFilters({
  brands,
  bounds,
  filters,
  count,
  isUpdating,
  onBrandChange,
  onPriceChange,
  onPowerChange,
  onSpeedChange,
  onAccelChange,
  onSeatsChange,
  onClear,
}: ModelFiltersProps) {
  const hasFilters = Object.keys(filters).length > 0
  return (
    <div className="filter-bar">
      <div className="filter-bar-head">
        <p className="eyebrow">Refine the collection</p>
        <div className="filter-meta">
          <span aria-live="polite">{count} models{isUpdating ? ' · updating' : ''}</span>
          {hasFilters && (
            <button className="filter-clear" type="button" onClick={onClear}>Clear filters</button>
          )}
        </div>
      </div>
      <div className="filter-controls">
        <label className="brand-filter">
          <span className="filter-label">Brand</span>
          <select
            aria-label="Filter by brand"
            value={filters.brand ?? ''}
            onChange={(event) => onBrandChange(event.target.value || undefined)}
          >
            <option value="">All brands</option>
            {brands.map((brand) => <option key={brand} value={brand}>{brand}</option>)}
          </select>
        </label>
        <RangeFilter
          label="Price"
          min={bounds.price[0]}
          max={bounds.price[1]}
          step={50000}
          valueMin={filters.minBasePriceCents}
          valueMax={filters.maxBasePriceCents}
          format={formatGBP}
          onChange={onPriceChange}
        />
        <RangeFilter
          label="Power (PS)"
          min={bounds.power[0]}
          max={bounds.power[1]}
          step={10}
          valueMin={filters.minPowerPs}
          valueMax={filters.maxPowerPs}
          format={(value) => `${value} PS`}
          onChange={onPowerChange}
        />
        <RangeFilter
          label="Top speed (km/h)"
          min={bounds.speed[0]}
          max={bounds.speed[1]}
          step={5}
          valueMin={filters.minTopSpeedKph}
          valueMax={filters.maxTopSpeedKph}
          format={(value) => `${value} km/h`}
          onChange={onSpeedChange}
        />
        <RangeFilter
          label="0-100 (s)"
          min={bounds.accel[0]}
          max={bounds.accel[1]}
          step={0.1}
          valueMin={filters.minAccelerationS}
          valueMax={filters.maxAccelerationS}
          format={(value) => `${value.toFixed(1)} s`}
          onChange={onAccelChange}
        />
        <RangeFilter
          label="Seats"
          min={bounds.seats[0]}
          max={bounds.seats[1]}
          step={1}
          valueMin={filters.minSeats}
          valueMax={filters.maxSeats}
          format={(value) => `${value}`}
          onChange={onSeatsChange}
        />
      </div>
    </div>
  )
}

/* ---------- Stage 1: model gallery ---------- */

interface ModelCardProps {
  model: VehicleModel
  index: number
  onSelect: () => void
}

function ModelCard({ model, index, onSelect }: ModelCardProps) {
  return (
    <article className="model-card">
      <div className="card-topline">
        <span className="model-index">{String(index + 1).padStart(2, '0')}</span>
        <span className="model-meta">{model.brand} · {model.modelYear}</span>
      </div>
      <h3>{model.name}</h3>
      <p className="card-desc">{model.description}</p>
      <dl className="card-specs">
        <div className="spec-engine"><dt>Engine</dt><dd>{model.engine}</dd></div>
        <div><dt>Power</dt><dd>{model.powerPs} PS</dd></div>
        <div><dt>0-100 km/h</dt><dd>{model.accelerationS.toFixed(1)} s</dd></div>
        <div><dt>Top speed</dt><dd>{model.topSpeedKph} km/h</dd></div>
        <div><dt>Drivetrain</dt><dd>{model.drivetrain}</dd></div>
        <div><dt>Range</dt><dd>{model.rangeKm != null ? `${model.rangeKm} km` : '—'}</dd></div>
        <div><dt>Seats</dt><dd>{model.seats}</dd></div>
      </dl>
      <div className="card-foot">
        <span className="model-price">From {formatGBP(model.basePriceCents)}</span>
        <button className="card-cta" type="button" aria-label={`Configure ${model.name}`} onClick={onSelect}>
          Configure <ArrowRight size={15} aria-hidden="true" />
        </button>
      </div>
    </article>
  )
}

interface ModelGalleryProps {
  models: VehicleModel[]
  catalog: VehicleModel[]
  filters: ModelFilter
  isUpdating: boolean
  onSelect: (model: VehicleModel) => void
  onFiltersChange: (next: ModelFilter) => void
  onClearFilters: () => void
}

function ModelGallery({ models, catalog, filters, isUpdating, onSelect, onFiltersChange, onClearFilters }: ModelGalleryProps) {
  const source = catalog.length > 0 ? catalog : models
  const brands = useMemo(() => Array.from(new Set(source.map((m) => m.brand))).sort(), [source])
  const bounds = useMemo(() => computeBounds(source), [source])

  const setBrand = (brand: string | undefined) => {
    const next = { ...filters }
    if (brand) next.brand = brand
    else delete next.brand
    onFiltersChange(next)
  }

  const setRange =
    (minKey: keyof ModelFilter, maxKey: keyof ModelFilter) => (min: number, max: number) =>
      onFiltersChange({ ...filters, [minKey]: min, [maxKey]: max })

  return (
    <section className="configurator gallery-section" id="collection" aria-label="Model collection">
      <div className="section-intro">
        <p className="eyebrow">The collection</p>
        <h2>Choose the form</h2>
        <p>Every commission begins with a silhouette. Select a model to open its specification.</p>
      </div>

      <ModelFilters
        brands={brands}
        bounds={bounds}
        filters={filters}
        count={models.length}
        isUpdating={isUpdating}
        onBrandChange={setBrand}
        onPriceChange={setRange('minBasePriceCents', 'maxBasePriceCents')}
        onPowerChange={setRange('minPowerPs', 'maxPowerPs')}
        onSpeedChange={setRange('minTopSpeedKph', 'maxTopSpeedKph')}
        onAccelChange={setRange('minAccelerationS', 'maxAccelerationS')}
        onSeatsChange={setRange('minSeats', 'maxSeats')}
        onClear={onClearFilters}
      />

      {models.length === 0 ? (
        <p className="filter-empty">No models match the current filters. Widen a range or clear the filters to see the full collection.</p>
      ) : (
        <div className="card-grid">
          {models.map((model, index) => (
            <ModelCard key={model.id} model={model} index={index} onSelect={() => onSelect(model)} />
          ))}
        </div>
      )}
    </section>
  )
}

/* ---------- Stage 2: configure the chosen model ---------- */

interface ConfiguratorProps {
  model: VehicleModel
  onBack: () => void
  onSaved: (configuration: Configuration) => void
}

function Configurator({ model, onBack, onSaved }: ConfiguratorProps) {
  const [trimId, setTrimId] = useState('')
  const [optionIds, setOptionIds] = useState<string[]>([])

  const trim = model.trims.find((item) => item.id === trimId)
  const groupedOptions = useMemo(() => {
    const groups = new Map<string, typeof model.options>()
    model.options.forEach((option) => {
      const group = groups.get(option.category) ?? []
      group.push(option)
      groups.set(option.category, group)
    })
    return Array.from(groups.entries())
  }, [model])

  const optionTotal = model.options
    .filter((option) => optionIds.includes(option.id))
    .reduce((sum, option) => sum + option.priceCents, 0)
  const total = model.basePriceCents + (trim?.priceDeltaCents ?? 0) + optionTotal

  const saveMutation = useMutation({
    mutationFn: createConfiguration,
    onSuccess: onSaved,
  })

  const toggleOption = (optionId: string) => {
    setOptionIds((current) =>
      current.includes(optionId)
        ? current.filter((id) => id !== optionId)
        : [...current, optionId],
    )
  }

  const save = () => {
    if (!trim) return
    saveMutation.mutate({ modelId: model.id, trimId: trim.id, optionIds })
  }

  return (
    <section className="configurator" id="configure" aria-label="Vehicle configurator">
      <div className="configurator-top">
        <button className="back-button" type="button" onClick={onBack}>
          <ChevronLeft size={15} aria-hidden="true" /> Change model
        </button>
        <div className="configurator-model">
          <span className="eyebrow">{model.brand} · {model.modelYear}</span>
          <h2>{model.name}</h2>
        </div>
      </div>

      <div className="config-layout">
        <div className="choices">
          <fieldset className="choice-section trim-section">
            <legend className="sr-only">Choose a trim</legend>
            <StepHeading number="01" title="Set the character" note={`Expressions available for the ${model.name}.`} />
            {model.trims.length > 0 ? (
              <div className="trim-list">
                {model.trims.map((item) => (
                  <label className={`trim-choice ${trimId === item.id ? 'is-selected' : ''}`} key={item.id}>
                    <input
                      type="radio"
                      name="trim"
                      value={item.id}
                      checked={trimId === item.id}
                      onChange={() => {
                        setTrimId(item.id)
                        saveMutation.reset()
                      }}
                    />
                    <span className="radio-mark" aria-hidden="true" />
                    <span><strong>{item.name}</strong><small>{item.priceDeltaCents === 0 ? 'Included' : `+ ${formatGBP(item.priceDeltaCents)}`}</small></span>
                  </label>
                ))}
              </div>
            ) : <p className="inline-empty">No trim editions are currently available for this model.</p>}
          </fieldset>

          <fieldset className="choice-section option-section">
            <legend className="sr-only">Choose options</legend>
            <StepHeading number="02" title="Add the finishing notes" note="Considered details, selected individually." />
            {groupedOptions.length > 0 ? (
              <div className="option-groups">
                {groupedOptions.map(([category, options]) => (
                  <div className="option-group" key={category}>
                    <h3>{category}</h3>
                    {options.map((option) => (
                      <label className="option-choice" key={option.id}>
                        <input
                          type="checkbox"
                          checked={optionIds.includes(option.id)}
                          onChange={() => toggleOption(option.id)}
                        />
                        <span className="checkbox-mark" aria-hidden="true"><Check size={13} /></span>
                        <span>{option.name}</span>
                        <small>+ {formatGBP(option.priceCents)}</small>
                      </label>
                    ))}
                  </div>
                ))}
              </div>
            ) : <p className="inline-empty">This model is complete as standard, with no additional options required.</p>}
          </fieldset>
        </div>

        <aside className="summary" aria-label="Configuration summary">
          <div className="summary-top">
            <span className="eyebrow">Your commission</span>
            <span className="summary-count">{optionIds.length} {optionIds.length === 1 ? 'option' : 'options'}</span>
          </div>
          <h2>{model.name}</h2>
          <dl>
            <div><dt>Base vehicle</dt><dd>{formatGBP(model.basePriceCents)}</dd></div>
            <div><dt>Edition</dt><dd>{trim ? trim.name : 'Not selected'}</dd></div>
            {trim && trim.priceDeltaCents > 0 && <div><dt>Edition adjustment</dt><dd>{formatGBP(trim.priceDeltaCents)}</dd></div>}
            <div><dt>Selected options</dt><dd>{formatGBP(optionTotal)}</dd></div>
          </dl>
          <div className="total-row">
            <span>Commission total</span>
            <strong>{formatGBP(total)}</strong>
            <small>Including VAT</small>
          </div>
          {saveMutation.isError && (
            <p className="save-error" role="alert">We could not save your configuration. Please try again.</p>
          )}
          <button
            className="save-button"
            type="button"
            disabled={!trim || saveMutation.isPending}
            onClick={save}
          >
            <span>{saveMutation.isPending ? 'Saving configuration…' : 'Save configuration'}</span>
            <ArrowRight size={17} aria-hidden="true" />
          </button>
          <p className="summary-footnote">Saving creates a reference for your specification. No payment is taken.</p>
        </aside>
      </div>
    </section>
  )
}

function SuccessState({ configuration, onReset }: { configuration: Configuration; onReset: () => void }) {
  return (
    <section className="success-state" id="configure" aria-live="polite">
      <div className="success-seal" aria-hidden="true"><Check size={30} /></div>
      <p className="eyebrow">Commission reference · {configuration.id}</p>
      <h2>Configuration saved</h2>
      <p className="success-lead">Your {configuration.model.name} specification is safely recorded.</p>
      <div className="receipt">
        <div className="receipt-title">
          <div><span>Model</span><strong>{configuration.model.name}</strong></div>
          <div><span>Edition</span><strong>{configuration.trim.name}</strong></div>
        </div>
        {configuration.options.length > 0 && (
          <div className="receipt-options">
            <span>Selected details</span>
            <ul>{configuration.options.map((option) => <li key={option.name}>{option.name} <small>{formatGBP(option.priceCents)}</small></li>)}</ul>
          </div>
        )}
        <div className="receipt-total">
          <span>Total, including VAT</span>
          <strong>{formatGBP(configuration.totalPriceCents)}</strong>
        </div>
        <p>Saved {formatDate(configuration.createdAt)} · Status: {configuration.status}</p>
      </div>
      <button className="secondary-button" type="button" onClick={onReset}>
        Configure another <RotateCcw size={15} aria-hidden="true" />
      </button>
    </section>
  )
}

function Footer() {
  return (
    <footer>
      <span>Aureline Motor House</span>
      <span>Built around the individual.</span>
      <a href="#top">Return to top</a>
    </footer>
  )
}

export default function App() {
  const [selectedModel, setSelectedModel] = useState<VehicleModel | null>(null)
  const [savedConfiguration, setSavedConfiguration] = useState<Configuration | null>(null)
  const [draftFilter, setDraftFilter] = useState<ModelFilter>({})
  const [catalog, setCatalog] = useState<VehicleModel[]>([])
  const filter = useDebouncedValue(draftFilter, 250)

  const modelsQuery = useQuery({
    queryKey: ['models', filter],
    queryFn: () => fetchModels(filter),
    placeholderData: keepPreviousData,
  })

  // Keep an unfiltered snapshot of the catalogue so the brand options and
  // slider bounds stay stable while a filtered result is on screen.
  useEffect(() => {
    if (modelsQuery.data && Object.keys(filter).length === 0) {
      setCatalog(modelsQuery.data)
    }
  }, [modelsQuery.data, filter])

  const returnToGallery = () => {
    setSavedConfiguration(null)
    setSelectedModel(null)
  }

  const hasDraftFilters = Object.keys(draftFilter).length > 0

  return (
    <>
      <Header />
      <main>
        <Hero />
        {modelsQuery.isLoading && <LoadingState />}
        {modelsQuery.isError && <ErrorState onRetry={() => void modelsQuery.refetch()} />}
        {modelsQuery.data && !modelsQuery.isError && (
          modelsQuery.data.length > 0 || hasDraftFilters ? (
            savedConfiguration
              ? <SuccessState configuration={savedConfiguration} onReset={returnToGallery} />
              : selectedModel
                ? <Configurator model={selectedModel} onBack={returnToGallery} onSaved={setSavedConfiguration} />
                : <ModelGallery
                    models={modelsQuery.data}
                    catalog={catalog}
                    filters={draftFilter}
                    isUpdating={modelsQuery.isFetching}
                    onSelect={setSelectedModel}
                    onFiltersChange={setDraftFilter}
                    onClearFilters={() => setDraftFilter({})}
                  />
          ) : (
            <EmptyState />
          )
        )}
      </main>
      <Footer />
    </>
  )
}
