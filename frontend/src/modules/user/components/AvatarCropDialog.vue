<template>
	<el-dialog
		:model-value="modelValue"
		title="裁剪头像"
		width="min(92vw, 560px)"
		destroy-on-close
		@closed="destroyCropper"
		@update:model-value="emit('update:modelValue', $event)"
	>
		<div class="avatar-crop-dialog">
			<div
				ref="cropperContainer"
				class="avatar-crop-dialog__canvas"
			></div>
		</div>
		<template #footer>
			<el-button
				:disabled="!cropper"
				@click="resetCrop"
				>重置</el-button
			>
			<el-button @click="emit('update:modelValue', false)">取消</el-button>
			<el-button
				type="primary"
				:disabled="!cropper"
				@click="confirmCrop"
				>确定</el-button
			>
		</template>
	</el-dialog>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { createCropper, type Cropper } from 'cropperjs';

const props = defineProps<{
	modelValue: boolean;
	sourceFile: File | null;
}>();

const emit = defineEmits<{
	'update:modelValue': [value: boolean];
	confirm: [file: File];
	error: [];
}>();

const cropperContainer = ref<HTMLElement>();
const cropper = ref<Cropper>();
const INITIAL_SELECTION_COVERAGE = 0.8;
let sourceUrl = '';
let initialSelection = { x: 0, y: 0, width: 0, height: 0 };

watch(
	() => [props.modelValue, props.sourceFile] as const,
	async ([visible, file]) => {
		destroyCropper();
		if (!visible || !file) return;

		sourceUrl = URL.createObjectURL(file);
		await nextTick();
		const image = new Image();
		image.src = sourceUrl;
		try {
			await image.decode();
		} catch {
			emit('error');
			destroyCropper();
			return;
		}
		if (!props.modelValue || !cropperContainer.value) return;

		cropper.value = createCropper(image, {
			container: cropperContainer.value,
			template:
				'<cropper-canvas background><cropper-image initial-fit="cover" max-inset="0" rotatable scalable translatable></cropper-image>' +
				'<cropper-handle action="move" theme-color="transparent"></cropper-handle>' +
				'<cropper-shade hidden></cropper-shade><cropper-selection min-inset="0" aspect-ratio="1" movable resizable>' +
				'<cropper-grid role="grid" bordered covered></cropper-grid><cropper-crosshair centered></cropper-crosshair>' +
				'<cropper-handle action="move" theme-color="rgba(255,255,255,.4)"></cropper-handle>' +
				'<cropper-handle action="n-resize"></cropper-handle><cropper-handle action="e-resize"></cropper-handle>' +
				'<cropper-handle action="s-resize"></cropper-handle><cropper-handle action="w-resize"></cropper-handle>' +
				'<cropper-handle action="ne-resize"></cropper-handle><cropper-handle action="nw-resize"></cropper-handle>' +
				'<cropper-handle action="se-resize"></cropper-handle><cropper-handle action="sw-resize"></cropper-handle>' +
				'</cropper-selection></cropper-canvas>',
		});
		await initializeSelection();
	},
	{ immediate: true },
);

async function initializeSelection() {
	const currentCropper = cropper.value;
	const container = cropperContainer.value;
	const image = currentCropper?.getCropperImage();
	const canvas = currentCropper?.getCropperCanvas();
	const selection = currentCropper?.getCropperSelection();
	if (!container || !image || !canvas || !selection) return;

	await image.$ready();
	await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
	if (!props.modelValue || cropper.value !== currentCropper) return;

	const canvasRect = canvas.getBoundingClientRect();
	const imageRect = image.getBoundingClientRect();
	const size =
		Math.min(canvasRect.width, canvasRect.height, imageRect.width, imageRect.height) *
		INITIAL_SELECTION_COVERAGE;
	initialSelection = {
		x: (canvasRect.width - size) / 2,
		y: (canvasRect.height - size) / 2,
		width: size,
		height: size,
	};
	selection.$change(
		initialSelection.x,
		initialSelection.y,
		initialSelection.width,
		initialSelection.height,
		1,
	);
}

function resetCrop() {
	const image = cropper.value?.getCropperImage();
	const selection = cropper.value?.getCropperSelection();
	if (!image || !selection) return;

	image.$resetTransform();
	image.$center('cover');
	selection.$change(
		initialSelection.x,
		initialSelection.y,
		initialSelection.width,
		initialSelection.height,
		1,
	);
}

async function confirmCrop() {
	const selection = cropper.value?.getCropperSelection();
	if (!selection) return;

	const canvas = await selection.$toCanvas({ width: 512, height: 512 });
	const blob = await new Promise<Blob | null>((resolve) =>
		canvas.toBlob(resolve, 'image/webp', 0.92),
	);
	if (!blob) return;
	emit('confirm', new File([blob], 'avatar.webp', { type: 'image/webp' }));
	emit('update:modelValue', false);
}

function destroyCropper() {
	cropper.value?.destroy();
	cropper.value = undefined;
	if (sourceUrl) URL.revokeObjectURL(sourceUrl);
	sourceUrl = '';
}

onBeforeUnmount(destroyCropper);
</script>

<style lang="scss" scoped>
.avatar-crop-dialog {
	&__canvas {
		height: min(62vh, 420px);
		min-height: 260px;
		overflow: hidden;
		background: var(--app-bg);
	}

	&__canvas :deep(cropper-canvas) {
		display: block;
		width: 100%;
		height: 100%;
	}
}
</style>
