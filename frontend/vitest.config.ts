import path from "node:path";
import { defineConfig } from "vitest/config";

export default defineConfig({
  esbuild: {
    jsx: "automatic"
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, ".")
    }
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./test/setup.ts"],
    css: false,
    include: [
      "app/**/*.test.ts",
      "components/**/*.test.tsx",
      "lib/**/*.test.ts"
    ],
    exclude: ["e2e/**"],
    coverage: {
      reporter: ["text", "lcov"]
    }
  }
});
