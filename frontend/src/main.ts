import { createApp } from 'vue';
import { registerTheme } from 'echarts/core';
import darkTheme from '@/modules/admin/dashboard/theme/dark.json';
import vintageTheme from '@/modules/admin/dashboard/theme/vintage.json';
import App from './App.vue';
import '@/assets/styles/tailwind.css';
import '@/assets/styles/index.scss';
import { setupTheme } from '@/composables/useTheme';
import router from '@/router/index';
import pinia from '@/stores/index';
import setupRouterGuards from '@/router/guards';
import 'md-editor-v3/lib/style.css';
// md 主题
import 'github-markdown-css/github-markdown.css';
// prism 代码主题 css
// import 'prismjs/themes/prism-okaidia.css';
import 'prism-themes/themes/prism-vsc-dark-plus.css';
// prism 插件的 css
import 'prismjs/plugins/toolbar/prism-toolbar.css';
import 'prismjs/plugins/line-numbers/prism-line-numbers.css';

import '@/assets/styles/markdown.scss';

// ECharts 主题需要在应用初始化时注册，所有图表组件才能通过主题名称复用。
registerTheme('vintage', vintageTheme);
registerTheme('dark', darkTheme);

const app = createApp(App);

setupTheme();
app.use(pinia);

// 因为 Vue Router 安装时会触发初始导航，守卫如果在 app.use(router) 之后才注册，首屏导航可能已经跑过了
setupRouterGuards(router);
app.use(router);

app.mount('#app');
