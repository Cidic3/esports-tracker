import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Forward API calls to the Spring Boot backend so the browser only ever
      // talks to one origin in dev — no CORS config needed on the backend.
      '/api': 'http://localhost:8080',
    },
  },
})
