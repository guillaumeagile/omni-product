import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';
import prettierRecommended from 'eslint-plugin-prettier/recommended';
import globals from 'globals';

export default tseslint.config(
    {
        // Only lint the Nest app and its e2e suite. Everything else in the repo
        // (vendored slide decks, legacy Java port, generated output, docs) is out of scope.
        ignores: [
            'dist/**',
            '.claude/**',
            'coverage/**',
            'reports/**',
            '.stryker-tmp/**',
            'prez-slides/**',
            'from-java-legacy/**',
            'agents/**',
            'docs/**',
            'graphify-out/**',
            'prisma/generated/**',
            'eslint.config.mjs',
        ],
    },
    eslint.configs.recommended,
    ...tseslint.configs.recommendedTypeChecked,
    {
        languageOptions: {
            parserOptions: {
                projectService: true,
                tsconfigRootDir: import.meta.dirname,
            },
            globals: {
                ...globals.node,
            },
        },
        rules: {
            '@typescript-eslint/no-unused-vars': ['error', {argsIgnorePattern: '^_', varsIgnorePattern: '^_'}],
            '@typescript-eslint/no-explicit-any': 'error',
            '@typescript-eslint/consistent-type-imports': ['error', {prefer: 'type-imports'}],
        },
    },
    {
        files: ['**/*.spec.ts', '**/*.e2e-spec.ts', 'test/**/*.ts', 'test-e2e/**/*.ts'],
        rules: {
            // Test doubles and HTTP-assertion chains (supertest on Nest's untyped
            // getHttpServer()) surface a lot of `any`; not worth fighting in specs.
            '@typescript-eslint/no-unsafe-assignment': 'off',
            '@typescript-eslint/no-unsafe-member-access': 'off',
            '@typescript-eslint/no-unsafe-argument': 'off',
            // `async` callbacks passed to async combinators document intent and
            // satisfy the Promise-returning signature even without an inner await.
            '@typescript-eslint/require-await': 'off',
        },
    },
    prettierRecommended,
);
