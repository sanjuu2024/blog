import { createApp } from 'vue';
import App from './App.vue';
import '@/assets/styles/tailwind.css';
import '@/assets/styles/index.scss';
import { setupTheme } from '@/composables/useTheme';
import router from '@/router/index.ts';

const app = createApp(App);

setupTheme();
app.use(router);

app.mount('#app');
