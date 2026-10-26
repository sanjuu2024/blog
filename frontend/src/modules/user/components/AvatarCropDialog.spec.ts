import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createCropper } from 'cropperjs';
import AvatarCropDialog from './AvatarCropDialog.vue';

let imageScale = 1;

const cropperImage = {
	$ready: vi.fn().mockResolvedValue(undefined),
	$resetTransform: vi.fn(() => {
		imageScale = 1;
	}),
	$center: vi.fn(),
	$move: vi.fn(),
	$getTransform: vi.fn(() => [imageScale, 0, 0, imageScale, 0, 0]),
	$zoom: vi.fn((scale: number) => {
		imageScale *= scale < 0 ? 1 / (1 - scale) : scale + 1;
	}),
	addEventListener: vi.fn(),
	removeEventListener: vi.fn(),
	getBoundingClientRect: vi.fn(() => ({ width: 560, height: 420, left: 0, top: 0 })),
};
const cropperSelection = {
	$change: vi.fn(),
	$toCanvas: vi.fn(),
};
const cropper = {
	destroy: vi.fn(),
	getCropperCanvas: vi.fn(() => ({
		getBoundingClientRect: vi.fn(() => ({ width: 560, height: 420, left: 0, top: 0 })),
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
		imageScale = 1;
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
		vi.stubGlobal('cancelAnimationFrame', vi.fn());
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
		expect(cropperOptions?.template).toContain('min-fit="cover"');
		expect(cropperOptions?.template).toContain('zoom-around-center');
		expect(cropperOptions?.template).toContain('min-inset="0"');
		expect(cropperOptions?.template).toContain('movable resizable');
		expect(cropperOptions?.template).toContain(
			'<cropper-handle action="move" theme-color="transparent"></cropper-handle>',
		);
		expect(cropperSelection.$change).toHaveBeenCalledWith(112, 42, 336, 336, 1);

		await wrapper.get('[data-testid="avatar-crop-reset"]').trigger('click');
		expect(cropperImage.$resetTransform).toHaveBeenCalled();
		expect(cropperImage.$center).toHaveBeenCalledWith('cover');
		expect(cropperSelection.$change).toHaveBeenCalledTimes(2);
	});

	it('shows only the vertical indicator for a tall image', async () => {
		cropperImage.getBoundingClientRect.mockReturnValue({
			width: 560,
			height: 840,
			left: 0,
			top: -120,
		} as DOMRect);
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
						template: '<button><slot /></button>',
					}),
				},
			},
		});

		await flushPromises();
		expect(wrapper.find('.avatar-crop-dialog__track--vertical').exists()).toBe(true);
		expect(wrapper.find('.avatar-crop-dialog__track--horizontal').classes()).toContain(
			'avatar-crop-dialog__track--hidden',
		);

		const verticalTrack = wrapper.get('.avatar-crop-dialog__track--vertical');
		Object.defineProperties(verticalTrack.element, {
			scrollHeight: { configurable: true, value: 840 },
			clientHeight: { configurable: true, value: 420 },
			scrollTop: { configurable: true, writable: true, value: 210 },
		});
		await verticalTrack.trigger('scroll');
		expect(cropperImage.$move).toHaveBeenCalledWith(0, -90);
	});

	it('clamps wheel zoom to the initial cover scale after zooming in', async () => {
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
					ElButton: true,
				},
			},
		});

		await flushPromises();
		imageScale = 3;
		const imageContainer = wrapper.get('.avatar-crop-dialog__canvas').element;
		for (let i = 0; i < 200; i++) {
			imageContainer.dispatchEvent(
				new WheelEvent('wheel', { deltaY: 1, bubbles: true, cancelable: true }),
			);
		}

		expect(imageScale).toBeCloseTo(1);
		expect(cropperImage.$zoom).toHaveBeenCalled();
		wrapper.unmount();
	});

	it('snaps exactly back to cover on the final wheel step', async () => {
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
					ElButton: true,
				},
			},
		});

		await flushPromises();
		imageScale = 1.005;
		const imageContainer = wrapper.get('.avatar-crop-dialog__canvas').element;
		imageContainer.dispatchEvent(
			new WheelEvent('wheel', { deltaY: 1, bubbles: true, cancelable: true }),
		);

		expect(cropperImage.$resetTransform).toHaveBeenCalled();
		expect(cropperImage.$center).toHaveBeenCalledWith('cover');
		expect(cropperImage.$zoom).not.toHaveBeenCalled();
		wrapper.unmount();
	});
});
