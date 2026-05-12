import { createApp } from 'vue';
import App from './App.vue';
import '@/assets/styles/tailwind.css';
import '@/assets/styles/index.scss';
import { setupTheme } from '@/composables/useTheme';

const app = createApp(App);

setupTheme();

app.mount('#app');
