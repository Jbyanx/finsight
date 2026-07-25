/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_TELEGRAM_BOT_USERNAME: string;
  // Agrega aquí las otras variables que estés usando en Login.tsx
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}