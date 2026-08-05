import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// Where the Vite dev server forwards /graphql. stack-up.sh serves the public
// BFF boundary on port 9090 by default; override with VITE_GRAPHQL_URL when
// running the stack on another WEB_PORT.
const graphqlTarget = process.env.VITE_GRAPHQL_URL ?? 'http://127.0.0.1:9090'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/graphql': graphqlTarget,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
})
