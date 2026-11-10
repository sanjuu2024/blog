import { mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NotFoundPage from './NotFoundPage.vue';

const router = vi.hoisted(() => ({
	back: vi.fn(),
	push: vi.fn(),
}));

vi.mock('vue-router', () => ({
	useRouter: () => router,
}));

const ElButtonStub = defineComponent({
	props: {
		type: String,
	},
	emits: ['click'],
	template: '<button :data-type="type" @click="$emit(\'click\')"><slot /></button>',
});

describe('NotFoundPage', () => {
	beforeEach(() => vi.clearAllMocks());

	it('shows the warm error layout, contained image, and navigation actions', async () => {
		const wrapper = mount(NotFoundPage, {
			global: {
				stubs: {
					ElButton: ElButtonStub,
				},
			},
		});

		expect(wrapper.get('.not-found-page__illustration').attributes('src')).toBe(
			'/img/404.avif',
		);
		expect(wrapper.get('.not-found-page__code').text()).toBe('404');
		expect(wrapper.get('.not-found-page__title').text()).toBe('页面不存在');
		expect(wrapper.findAll('button')).toHaveLength(2);
		expect(wrapper.get('.not-found-page__secondary-button').text()).toBe('返回上一页');
		expect(wrapper.get('.not-found-page__secondary-button').attributes('style')).toContain(
			'--el-button-text-color: #263238',
		);
		expect(wrapper.get('button[data-type="primary"]').text()).toBe('返回首页');

		await wrapper.get('.not-found-page__secondary-button').trigger('click');
		await wrapper.get('button[data-type="primary"]').trigger('click');

		expect(router.back).toHaveBeenCalledOnce();
		expect(router.push).toHaveBeenCalledWith('/');
		wrapper.unmount();
	});
});
