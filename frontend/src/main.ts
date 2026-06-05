import { createApp } from 'vue';
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

const app = createApp(App);

setupTheme();
app.use(pinia);

// 因为 Vue Router 安装时会触发初始导航，守卫如果在 app.use(router) 之后才注册，首屏导航可能已经跑过了
setupRouterGuards(router);
app.use(router);

app.mount('#app');
