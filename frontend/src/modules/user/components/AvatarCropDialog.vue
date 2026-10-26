<template>
	<el-dialog
		:model-value="modelValue"
		title="裁剪头像"
		width="min(92vw, 560px)"
		destroy-on-close
		@closed="destroyCropper"
		@update:model-value="emit('update:modelValue', $event)"
	>
		<div class="avatar-crop-dialog__stage">
			<div
				ref="cropperContainer"
				class="avatar-crop-dialog__canvas"
			></div>
			<div
				ref="verticalTrack"
				class="avatar-crop-dialog__track avatar-crop-dialog__track--vertical"
				:class="{ 'avatar-crop-dialog__track--hidden': !scrollbar.vertical.visible }"
				@scroll="handleTrackScroll('vertical')"
			>
				<div
					class="avatar-crop-dialog__scroll-content"
					:style="{ height: `${scrollbar.vertical.contentSize}px` }"
				></div>
			</div>
			<div
				ref="horizontalTrack"
				class="avatar-crop-dialog__track avatar-crop-dialog__track--horizontal"
				:class="{ 'avatar-crop-dialog__track--hidden': !scrollbar.horizontal.visible }"
				@scroll="handleTrackScroll('horizontal')"
			>
				<div
					class="avatar-crop-dialog__scroll-content"
					:style="{ width: `${scrollbar.horizontal.contentSize}px` }"
				></div>
			</div>
		</div>
		<template #footer>
			<el-button
				data-testid="avatar-crop-reset"
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
import { nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue';
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
// 接管滚轮缩放，在最后一步直接贴合 cover 边界。
const IMAGE_ZOOM_STEP = 0.01;
let sourceUrl = '';
let initialSelection = { x: 0, y: 0, width: 0, height: 0 };
let minimumImageScale = 1;
const scrollbar = reactive({
	vertical: { visible: false, contentSize: 0, offset: 0 },
	horizontal: { visible: false, contentSize: 0, offset: 0 },
});
const verticalTrack = ref<HTMLElement>();
const horizontalTrack = ref<HTMLElement>();
let cropperImageElement: Element | null = null;
let syncingTrack = false;
let scrollbarUpdateFrame = 0;

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
		// Cropper 默认在画布处理滚轮；在捕获阶段先处理，以便钳制离散缩放步长。
		cropperContainer.value.addEventListener('wheel', handleImageWheel, {
			capture: true,
			passive: false,
		});

		cropper.value = createCropper(image, {
			container: cropperContainer.value,
			template:
				'<cropper-canvas background><cropper-image initial-fit="cover" min-fit="cover" max-inset="0" zoom-around-center rotatable scalable translatable></cropper-image>' +
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
		cropperImageElement = cropper.value.getCropperImage();
		cropperImageElement?.addEventListener('transform', scheduleScrollbarUpdate);
		cropperImageElement?.addEventListener('change', scheduleScrollbarUpdate);
		window.addEventListener('resize', scheduleScrollbarUpdate);
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
	// 保存初始 cover 变换，作为后续滚轮缩放的精确下限。
	minimumImageScale = getImageScale(image);

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
	updateScrollbars();
}

function resetCrop() {
	const image = cropper.value?.getCropperImage();
	const selection = cropper.value?.getCropperSelection();
	if (!image || !selection) return;

	resetImageToCover(image);
	selection.$change(
		initialSelection.x,
		initialSelection.y,
		initialSelection.width,
		initialSelection.height,
		1,
	);
}

function handleImageWheel(event: WheelEvent) {
	const image = cropper.value?.getCropperImage();
	if (!image) return;

	event.preventDefault();
	event.stopPropagation();
	const currentScale = getImageScale(image);
	const requestedScale =
		event.deltaY > 0
			? currentScale / (1 + IMAGE_ZOOM_STEP)
			: currentScale * (1 + IMAGE_ZOOM_STEP);
	if (event.deltaY > 0 && requestedScale <= minimumImageScale * (1 + IMAGE_ZOOM_STEP)) {
		// 逐步缩小时可能在到达 cover 边界前被拒绝，因此最后一步直接恢复到 cover。
		resetImageToCover(image);
		return;
	}

	const nextScale = Math.max(minimumImageScale, requestedScale);
	if (Math.abs(nextScale - currentScale) < Number.EPSILON) return;

	const factor = nextScale / currentScale;
	image.$zoom(factor < 1 ? 1 - 1 / factor : factor - 1);
}

function resetImageToCover(image: NonNullable<ReturnType<Cropper['getCropperImage']>>) {
	image.$resetTransform();
	image.$center('cover');
	minimumImageScale = getImageScale(image);
	updateScrollbars();
}

function getImageScale(image: NonNullable<ReturnType<Cropper['getCropperImage']>>) {
	// 变换矩阵第一列可计算图片缩放比例，并包含旋转分量。
	const [scaleX, skewY] = image.$getTransform();
	return Math.hypot(scaleX, skewY);
}

function updateScrollbars() {
	const canvas = cropper.value?.getCropperCanvas();
	const image = cropper.value?.getCropperImage();
	if (!canvas || !image) return;

	const canvasRect = canvas.getBoundingClientRect();
	const imageRect = image.getBoundingClientRect();
	const canvasWidth = canvasRect.width;
	const canvasHeight = canvasRect.height;
	const imageLeft = imageRect.left - canvasRect.left;
	const imageTop = imageRect.top - canvasRect.top;
	// 原生滚动轨道只表示图片超出裁剪画布的部分。
	const horizontalRange = Math.max(0, imageRect.width - canvasWidth);
	const verticalRange = Math.max(0, imageRect.height - canvasHeight);

	setScrollbarState(
		scrollbar.horizontal,
		imageRect.width,
		canvasWidth,
		Math.min(horizontalRange, Math.max(0, -imageLeft)),
	);
	setScrollbarState(
		scrollbar.vertical,
		imageRect.height,
		canvasHeight,
		Math.min(verticalRange, Math.max(0, -imageTop)),
	);
}

function scheduleScrollbarUpdate() {
	if (scrollbarUpdateFrame) cancelAnimationFrame(scrollbarUpdateFrame);
	scrollbarUpdateFrame = requestAnimationFrame(() => {
		scrollbarUpdateFrame = 0;
		updateScrollbars();
	});
}

function setScrollbarState(
	state: { visible: boolean; contentSize: number; offset: number },
	contentSize: number,
	viewportSize: number,
	offset: number,
) {
	state.visible = contentSize > viewportSize + 1;
	state.contentSize = contentSize;
	if (!state.visible) {
		state.contentSize = 0;
		state.offset = 0;
		return;
	}
	const scrollRange = contentSize - viewportSize;
	state.offset = scrollRange > 0 ? offset / scrollRange : 0;
	void nextTick(syncNativeTracks);
}

function syncNativeTracks() {
	if (syncingTrack) return;
	// 同步原生滚动条与 Cropper 图片变换，并避免同步操作再次触发滚动处理。
	syncingTrack = true;
	if (verticalTrack.value && scrollbar.vertical.visible) {
		verticalTrack.value.scrollTop =
			(verticalTrack.value.scrollHeight - verticalTrack.value.clientHeight) *
			scrollbar.vertical.offset;
	}
	if (horizontalTrack.value && scrollbar.horizontal.visible) {
		horizontalTrack.value.scrollLeft =
			(horizontalTrack.value.scrollWidth - horizontalTrack.value.clientWidth) *
			scrollbar.horizontal.offset;
	}
	syncingTrack = false;
}

function handleTrackScroll(axis: 'vertical' | 'horizontal') {
	if (syncingTrack) return;
	const image = cropper.value?.getCropperImage();
	const canvas = cropper.value?.getCropperCanvas();
	const track = axis === 'vertical' ? verticalTrack.value : horizontalTrack.value;
	if (!image || !canvas || !track) return;

	const nativeRange =
		axis === 'vertical'
			? track.scrollHeight - track.clientHeight
			: track.scrollWidth - track.clientWidth;
	if (nativeRange <= 0) return;
	const ratio =
		axis === 'vertical' ? track.scrollTop / nativeRange : track.scrollLeft / nativeRange;
	const canvasRect = canvas.getBoundingClientRect();
	const imageRect = image.getBoundingClientRect();
	const currentScroll =
		axis === 'vertical'
			? Math.max(0, canvasRect.top - imageRect.top)
			: Math.max(0, canvasRect.left - imageRect.left);
	// 将原生轨道的归一化位置换算为 Cropper 图片平移距离。
	const targetScroll =
		ratio *
		((axis === 'vertical' ? imageRect.height : imageRect.width) -
			(axis === 'vertical' ? canvasRect.height : canvasRect.width));
	const movement = currentScroll - targetScroll;
	if (axis === 'vertical') image.$move(0, movement);
	else image.$move(movement, 0);
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
	cropperContainer.value?.removeEventListener('wheel', handleImageWheel, true);
	cropper.value?.destroy();
	cropper.value = undefined;
	cropperImageElement?.removeEventListener('transform', scheduleScrollbarUpdate);
	cropperImageElement?.removeEventListener('change', scheduleScrollbarUpdate);
	cropperImageElement = null;
	if (scrollbarUpdateFrame) cancelAnimationFrame(scrollbarUpdateFrame);
	scrollbarUpdateFrame = 0;
	window.removeEventListener('resize', scheduleScrollbarUpdate);
	scrollbar.vertical.visible = false;
	scrollbar.horizontal.visible = false;
	minimumImageScale = 1;
	if (sourceUrl) URL.revokeObjectURL(sourceUrl);
	sourceUrl = '';
}

onBeforeUnmount(destroyCropper);
</script>

<style lang="scss" scoped>
.avatar-crop-dialog__stage {
	display: grid;
	width: 100%;
	height: min(62vh, 420px);
	min-height: 260px;
	grid-template-columns: minmax(0, 1fr) 1rem;
	grid-template-rows: minmax(0, 1fr) 1rem;
}

.avatar-crop-dialog__canvas {
	min-width: 0;
	min-height: 0;
	grid-column: 1;
	grid-row: 1;
	overflow: hidden;
	background: var(--app-bg);
}

.avatar-crop-dialog__canvas :deep(cropper-canvas) {
	display: block;
	width: 100%;
	height: 100%;
}

.avatar-crop-dialog__track--vertical {
	grid-column: 2;
	grid-row: 1;
	overflow: hidden scroll;
}

.avatar-crop-dialog__track--horizontal {
	grid-column: 1;
	grid-row: 2;
	overflow: scroll hidden;
}

.avatar-crop-dialog__track--hidden {
	visibility: hidden;
	pointer-events: none;
}

.avatar-crop-dialog__track {
	min-width: 0;
	min-height: 0;
}
</style>
