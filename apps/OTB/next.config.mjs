/** @type {import('next').NextConfig} */
import { fileURLToPath } from 'url';
import path from 'path';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const nextConfig = {
  // Fix workspace root for monorepo (multiple lockfiles)
  outputFileTracingRoot: __dirname,

  // Disable ESLint during builds (workspace has lint errors)
  eslint: {
    ignoreDuringBuilds: true,
  },

  // Disable type checking during build (use tsc separately)
  typescript: {
    ignoreBuildErrors: true,
  },

  // Output standalone for Azure App Services (Node 22/24 compatible)
  output: 'standalone',

  // Disable image optimization (không dùng Vercel)
  images: {
    unoptimized: true,
  },

  // Transpile packages if needed for Node 22/24
  transpilePackages: [],

  // Logging
  logging: {
    fetches: {
      fullUrl: true,
    },
  },
};

export default nextConfig;
