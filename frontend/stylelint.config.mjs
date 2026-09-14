export default {
	extends: ['stylelint-config-standard-scss', 'stylelint-config-recommended-vue/scss'],
	customSyntax: 'postcss-html',
	ignoreFiles: ['**/node_modules/**', '**/dist/**', '**/coverage/**', '**/*.min.css', '**/*.map'],

	overrides: [
		{
			files: ['**/*.vue'],
			rules: {
				// postcss-html 会把模板中的 style 属性解析成孤立声明，对 Vue 合法内联样式产生误报。
				'no-invalid-position-declaration': null,
			},
		},
	],

	rules: {
		// 项目已有大量 BEM 和业务类名，不强制统一命名格式。
		'selector-class-pattern': null,

		// CSS 自定义变量使用 --app-*、--el-* 等多种既有命名空间。
		'custom-property-pattern': null,

		// Vue 和 Element Plus 中会使用 :deep()、:global()、:slotted()。
		'selector-pseudo-class-no-unknown': [
			true,
			{
				ignorePseudoClasses: ['deep', 'global', 'slotted'],
			},
		],

		// 项目使用 Sass 自定义 at-rule 和 Tailwind 相关语法。
		'at-rule-no-unknown': [
			true,
			{
				ignoreAtRules: [
					'use',
					'forward',
					'mixin',
					'include',
					'extend',
					'function',
					'return',
					'if',
					'else',
					'for',
					'each',
					'while',
					'tailwind',
					'apply',
					'layer',
				],
			},
		],

		// 先不把选择器顺序问题作为阻塞项，避免一次性改动大量历史样式。
		'no-descending-specificity': null,

		// 允许使用 -webkit-mask 等浏览器前缀属性，保留兼容性写法。
		'property-no-vendor-prefix': [
			true,
			{
				ignoreProperties: ['-webkit-mask'],
			},
		],
	},
};
