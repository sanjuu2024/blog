import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import pluginVue from 'eslint-plugin-vue';
import { defineConfig } from 'eslint/config';
import skipFormatting from 'eslint-config-prettier';

export default defineConfig([
	{ ignores: ['dist/**', 'node_modules/**', 'public/**'] },
	{
		files: ['**/*.{js,mjs,cjs,ts,mts,cts,vue}'],
		plugins: { js },
		extends: ['js/recommended'],
		languageOptions: { globals: globals.browser },
	},
	tseslint.configs.recommended,
	pluginVue.configs['flat/essential'],
	{
		files: ['**/*.vue'],
		languageOptions: { parserOptions: { parser: tseslint.parser } },
	},
	{
		rules: {
			// 允许未使用的变量警告（不阻塞编译）
			'@typescript-eslint/no-unused-vars': 'warn',
			// 生产环境禁用 console（当你在本地开发（dev）时，随便写 log 没关系；但当执行 npm run build 打包发布时，如果代码里残留了 console.log，ESLint 会直接报错拦截，强迫你删掉它们再发布。）
			'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
		},
	},
	skipFormatting, // 是对 eslint-config-prettier 的一个别名引用，这个配置会关掉 ESLint 中所有可能与 Prettier 冲突的视觉样式规则，把“美化代码”的权力彻底交给 Prettier。
]);
