import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createCropper } from 'cropperjs';
import AvatarCropDialog from './AvatarCropDialog.vue';

const cropperImage = {
	$ready: vi.fn().mockResolvedValue(undefined),
	$resetTransform: vi.fn(),
	$center: vi.fn(),
	getBoundingClientRect: vi.fn(() => ({ width: 560, height: 420 })),
};
const cropperSelection = {
	$change: vi.fn(),
	$toCanvas: vi.fn(),
};
const cropper = {
	destroy: vi.fn(),
	getCropperCanvas: vi.fn(() => ({
		getBoundingClientRect: vi.fn(() => ({ width: 560, height: 420 })),
	})),
	getCropperImage: vi.fn(() => cropperImage),
	getCropperSelection: vi.fn(() => cropperSelection),
};

vi.mock('cropperjs', () => ({
	createCropper: vi.fn(() => cropper),
}));

describe('AvatarCropDialog', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.stubGlobal(
			'URL',
			class extends URL {
				static createObjectURL = vi.fn(() => 'blob:avatar');
				static revokeObjectURL = vi.fn();
			},
		);
		vi.stubGlobal(
			'Image',
			class {
				src = '';
				decode = vi.fn().mockResolvedValue(undefined);
			},
		);
		vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
			callback(0);
			return 0;
		});
	});

	it('initializes a centered crop area and drags the image inside a fixed frame', async () => {
		const wrapper = mount(AvatarCropDialog, {
			props: {
				modelValue: true,
				sourceFile: new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' }),
			},
			global: {
				stubs: {
					ElDialog: defineComponent({
						template: '<div><slot /><slot name="footer" /></div>',
					}),
					ElButton: defineComponent({
						emits: ['click'],
						template: '<button @click="$emit(\'click\')"><slot /></button>',
					}),
				},
			},
		});

		await flushPromises();
		expect(createCropper).toHaveBeenCalled();
		const cropperOptions = vi.mocked(createCropper).mock.calls[0]?.[1];
		expect(cropperOptions?.template).toContain('max-inset="0"');
		expect(cropperOptions?.template).toContain('min-inset="0"');
		expect(cropperOptions?.template).toContain('movable resizable');
		expect(cropperOptions?.template).toContain(
			'<cropper-handle action="move" theme-color="transparent"></cropper-handle>',
		);
		expect(cropperSelection.$change).toHaveBeenCalledWith(112, 42, 336, 336, 1);

		await wrapper.get('button').trigger('click');
		expect(cropperImage.$resetTransform).toHaveBeenCalled();
		expect(cropperImage.$center).toHaveBeenCalledWith('cover');
		expect(cropperSelection.$change).toHaveBeenCalledTimes(2);
	});
});
