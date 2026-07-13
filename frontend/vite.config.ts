/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import Icons from 'unplugin-icons/vite';
import IconsResolver from 'unplugin-icons/resolver';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
	const env = loadEnv(mode, process.cwd());
	return {
		plugins: [
			vue(),
			tailwindcss(),
			// 1. 自动导入 vue 等库的 API 和 Element Plus 函数
			AutoImport({
				imports: ['vue', 'vue-router', 'pinia'], // 自动导入 vue 等库的 API
				resolvers: [
					ElementPlusResolver(),
					// 自动导入图标组件
					IconsResolver({
						prefix: 'Icon',
					}),
				],
				eslintrc: {
					enabled: true, // 生成 .eslintrc-auto-import.json
					filepath: './.eslintrc-auto-import.json',
					globalsPropValue: true, // 默认值为 true
				},
				dts: 'src/auto-import.d.ts', // 生成类型声明文件，防止 TS 报错
			}),

			// 2. 自动导入 Element Plus 组件
			Components({
				resolvers: [
					ElementPlusResolver(),
					// 自动注册图标组件（前缀为 i-ep-、i-solar-）
					IconsResolver({
						enabledCollections: [
							'ep',
							'solar',
							'lets-icons',
							'material-symbols',
							'mdi',
							'lucide',
						],
					}),
				],
				dts: 'src/components.d.ts',
			}),

			// 3. 图标库核心配置
			Icons({
				autoInstall: true, // 如果发现代码中的图标组件不存在，则自动安装对应的图标库如@iconify-json/ant-design
			}),
		],
		resolve: {
			alias: {
				'@': path.resolve(__dirname, './src'),
			},
		},

		css: {
			preprocessorOptions: {
				scss: {
					additionalData: `@use "@/assets/styles/variables.scss" as *;`,
				},
			},
		},

		server: {
			proxy: {
				[env.VITE_API_BASE_URL]: {
					target: env.VITE_SERVER,
					changeOrigin: true,
				},
			},
		},

		test: {
			environment: 'jsdom',
			setupFiles: ['./src/test/setup.ts'],
			include: ['src/**/*.spec.ts'],
			server: {
				deps: {
					inline: ['element-plus'],
				},
			},
			coverage: {
				provider: 'v8',
				reporter: ['text', 'html'],
				include: ['src/**/*.{ts,vue}'],
				exclude: ['src/**/*.d.ts', 'src/**/*.spec.ts', 'src/test/**', 'src/main.ts'],
			},
		},
	};
});
