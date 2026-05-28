import { createApp } from 'vue';
import App from './App.vue';
import '@/assets/styles/tailwind.css';
import '@/assets/styles/index.scss';
import { setupTheme } from '@/composables/useTheme';
import router from '@/router/index';
import pinia from '@/stores/index';
import setupRouterGuards from '@/router/guards';
import 'md-editor-v3/lib/style.css';

const app = createApp(App);

setupTheme();
app.use(pinia);

// 因为 Vue Router 安装时会触发初始导航，守卫如果在 app.use(router) 之后才注册，首屏导航可能已经跑过了
setupRouterGuards(router);
app.use(router);

app.mount('#app');
