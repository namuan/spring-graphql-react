import { test, expect } from '@playwright/test'

// Outside-in browser tests: drive the real public UI (React SPA served by
// nginx) through the real GraphQL orchestrator into the real vehicle
// service and its Postgres database, all running in the Podman pod.
//
// The app is a two-stage flow:
//   Stage 1 - pick a model from its specification card (each card carries
//             the tech spec seeded by V4).
//   Stage 2 - configure trim + options, then save.
//
// The spec deliberately uses accessible roles and conservative selectors:
// model cards expose a button whose accessible name is "Configure <model>",
// trim choices are radio buttons, option packages are checkboxes, and the
// CTA is a button named "Save configuration".
//
// Source of truth for the seed data below:
//   vehicle-config-service/src/main/resources/db/migration/V2__seed_vehicle_catalog.sql
//   vehicle-config-service/src/main/resources/db/migration/V4__add_vehicle_model_tech_specs.sql

const BASE_URL = process.env.BASE_URL ?? 'http://127.0.0.1:8080'

// Seeded catalog: brand 'Aster', models 'Vale' and 'Terra' are rendered as
// cards whose Configure buttons carry the accessible names "Configure Vale"
// and "Configure Terra".
const SEEDED_MODEL = /Vale/
const SEEDED_MODEL_2 = /Terra/
const TRIM_APEX = /Apex/
const OPTION_CANOPY = /Panoramic canopy/

// V4 seeds the Aster Vale with 520 PS (unique in the catalogue), so the exact
// text appears only on its card.
const VALE_SPEC_POWER = '520 PS'

// Expected total for the configured build:
//   4,590,000 (Aster Vale base) + 610,000 (Apex edition)
//   + 145,000 (Panoramic canopy) = 5,345,000 cents = GBP 53,450
// Rendered with Intl 'en-GB' GBP, maximumFractionDigits: 0 (see
// frontend/src/lib/format.ts). Written as a unicode escape so this file
// stays ASCII-only.
const EXPECTED_TOTAL = '\u00A353,450'

test('base page reaches real seeded data', async ({ page }) => {
  test.setTimeout(120_000)
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })

  // Two seeded models arrive from the real backend through
  // nginx -> orchestrator -> vehicle-config-service -> Postgres.
  await expect(page.getByRole('button', { name: SEEDED_MODEL })).toBeVisible({
    timeout: 90_000,
  })
  await expect(page.getByRole('button', { name: SEEDED_MODEL_2 })).toBeVisible()

  // Tech specs seeded by V4 render on the cards. "520 PS" is unique to the
  // Vale card; the "Power" label is asserted within that card because the
  // label repeats on every card.
  await expect(page.getByText(VALE_SPEC_POWER, { exact: true })).toBeVisible()
  const valeCard = page.getByRole('button', { name: SEEDED_MODEL }).locator('xpath=ancestor::article')
  await expect(valeCard.getByText('Power', { exact: true })).toBeVisible()
  await expect(valeCard.getByText('Twin-turbo V6', { exact: true })).toBeVisible()
})

test('configure a vehicle, save it, and reload to seeded data', async ({ page }) => {
  test.setTimeout(240_000)
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })

  // 1. Stage 1: choose the model from its card.
  const modelCard = page.getByRole('button', { name: SEEDED_MODEL })
  await expect(modelCard).toBeVisible({ timeout: 90_000 })
  await modelCard.click()

  // 2. Stage 2: trim 'Apex' (radio) and option 'Panoramic canopy'
  //    (checkbox). Only the selected model's trims/options are rendered.
  const trimRadio = page.getByRole('radio', { name: TRIM_APEX })
  await trimRadio.check()

  const canopyCheckbox = page.getByRole('checkbox', { name: OPTION_CANOPY })
  await canopyCheckbox.check()

  // 3. Save and wait for the success state.
  const saveButton = page.getByRole('button', { name: 'Save configuration' })
  await expect(saveButton).toBeEnabled()
  await saveButton.click()

  await expect(
    page.getByRole('heading', { name: 'Configuration saved' }),
  ).toBeVisible({ timeout: 60_000 })

  // 4. A config ID: the success panel shows "Commission reference"
  //    followed by the UUID returned by the backend.
  await expect(page.getByText(/Commission reference/)).toBeVisible()
  const body = await page.locator('body').innerText()
  const id = body.match(
    /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i,
  )?.[0]
  expect(id).toBeTruthy()

  // 5. A plausible formatted total: en-GB GBP with thousands grouping,
  //    and the exact sum for this deterministic build.
  expect(body).toMatch(/\u00A3\d{1,3}(?:,\d{3})+/)
  await expect(page.getByText(EXPECTED_TOTAL)).toBeVisible()

  // 6. Read the saved record back through the same public GraphQL boundary.
  // This verifies the PostgreSQL row and option join, not just the mutation
  // response shown by React.
  const response = await page.request.post(`${BASE_URL}/graphql`, {
    data: {
      query: `
        query SavedConfiguration($id: ID!) {
          configuration(id: $id) {
            id
            totalPriceCents
            model { name }
            trim { name }
            options { name }
          }
        }
      `,
      variables: { id },
    },
  })
  expect(response.ok()).toBeTruthy()
  const result = await response.json()
  expect(result.errors).toBeUndefined()
  expect(result.data.configuration).toMatchObject({
    id: id?.toLowerCase(),
    totalPriceCents: 5_345_000,
    model: { name: 'Vale' },
    trim: { name: 'Apex' },
    options: [{ name: 'Panoramic canopy' }],
  })

  // 7. Reload: the base page must still reach the real seeded data, with
  //    model cards (not radios) and their tech specs.
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: SEEDED_MODEL })).toBeVisible({
    timeout: 90_000,
  })
  await expect(page.getByRole('button', { name: SEEDED_MODEL_2 })).toBeVisible()
  await expect(page.getByText(VALE_SPEC_POWER, { exact: true })).toBeVisible()
})

test('filters narrow the collection through GraphQL', async ({ page }) => {
  test.setTimeout(120_000)
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })

  const valeCard = page.getByRole('button', { name: SEEDED_MODEL })
  await expect(valeCard).toBeVisible({ timeout: 90_000 })
  await expect(page.getByRole('button', { name: /Nera GT/ })).toBeVisible()

  // 1. Brand filter: wait for the GraphQL response carrying the brand so we
  //    prove the UI submitted the filter server-side rather than hiding cards
  //    locally.
  const brandResponse = page.waitForResponse((res) =>
    res.url().includes('/graphql')
    && res.request().postDataJSON()?.variables?.filter?.brand === 'Veloce',
  )
  await page.getByLabel('Filter by brand').selectOption('Veloce')
  await brandResponse

  await expect(page.getByRole('button', { name: /Nera GT/ })).toBeVisible({
    timeout: 15_000,
  })
  await expect(valeCard).toBeHidden()

  // 2. Tech-spec range slider: minimum power 600 PS. Within Veloce only the
  //    Zero EV (600) and Cento (640) qualify; the Nera GT (560) drops out.
  //    The value is set through the prototype setter so React treats it as a
  //    genuine user change (a direct `input.value` assignment is swallowed by
  //    React's controlled-input tracking and never fires onChange).
  const powerResponse = page.waitForResponse((res) =>
    res.url().includes('/graphql')
    && res.request().postDataJSON()?.variables?.filter?.minPowerPs === 600,
  )
  await page.getByLabel('Minimum power (PS)').evaluate((el) => {
    const input = el as HTMLInputElement
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set
    setter?.call(input, '600')
    input.dispatchEvent(new Event('input', { bubbles: true }))
  })
  await powerResponse

  await expect(page.getByRole('button', { name: /Nera GT/ })).toBeHidden()
  await expect(page.getByRole('button', { name: /Cento/ })).toBeVisible()
})
