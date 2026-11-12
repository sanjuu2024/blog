<template>
	<section
		class="sticker-picker"
		aria-label="预置表情选择器"
	>
		<header class="sticker-picker__header">
			<strong>{{ selectedPack?.name || '表情' }}</strong>
			<span
				v-if="selectedPack"
				class="sticker-picker__count"
				>{{ selectedPack.items.length }} 个</span
			>
			<button
				type="button"
				class="sticker-picker__close"
				aria-label="关闭表情选择器"
				@click="emit('close')"
			>
				<i-lucide-x />
			</button>
		</header>
		<div
			v-if="loading"
			class="sticker-picker__state"
		>
			正在加载表情…
		</div>
		<div
			v-else-if="failed"
			class="sticker-picker__state"
		>
			<span>表情加载失败</span>
			<el-button
				link
				type="primary"
				@click="loadStickers"
				>重试</el-button
			>
		</div>
		<template v-else-if="selectedPack">
			<div
				:key="selectedPack.id"
				class="sticker-picker__items app-scrollbar"
				:style="{ '--sticker-cell-min-width': `${selectedPack.gridMinWidthRem ?? 3}rem` }"
			>
				<button
					v-for="sticker in selectedPack.items"
					:key="sticker.token"
					type="button"
					class="sticker-picker__item"
					:aria-label="`插入表情：${sticker.name}`"
					@mousedown.prevent
					@click="emit('select', sticker)"
				>
					<StickerImage
						:name="sticker.name"
						:url="sticker.url"
					/>
				</button>
			</div>
			<div
				class="sticker-picker__packs app-scrollbar"
				aria-label="表情库"
			>
				<button
					v-for="pack in packs"
					:key="pack.id"
					type="button"
					class="sticker-picker__pack"
					:class="{ 'sticker-picker__pack--active': selectedPack.id === pack.id }"
					:aria-label="pack.name"
					:aria-pressed="selectedPack.id === pack.id"
					:title="pack.name"
					@mousedown.prevent
					@click="selectedId = pack.id"
				>
					<StickerImage
						:name="pack.name"
						:url="pack.iconUrl"
					/>
				</button>
			</div>
		</template>
	</section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useStickers } from '@/composables/useStickers';
import type { Sticker } from '@/types/sticker';
import StickerImage from '@/components/sticker/StickerImage.vue';

const { packs, loading, failed, loadStickers } = useStickers();
const selectedId = ref('');
const selectedPack = computed(
	() => packs.value.find((pack) => pack.id === selectedId.value) || packs.value[0],
);
const emit = defineEmits<{ select: [sticker: Sticker]; close: [] }>();
</script>

<style scoped lang="scss">
.sticker-picker {
	overflow: hidden;
	margin-top: 0.5rem;
	border: 1px solid var(--app-border);
	border-radius: 0.5rem;
	background: var(--app-bg);
	color: var(--app-text);
}

.sticker-picker__header {
	display: flex;
	align-items: center;
	gap: 0.65rem;
	padding: 0.6rem 0.8rem;
	border-bottom: 1px solid var(--app-border);
}

.sticker-picker__count {
	color: var(--app-text-muted);
	font-size: 0.85rem;
}

.sticker-picker__close {
	margin-left: auto;
	padding: 0.25rem;
	cursor: pointer;
}

.sticker-picker__items {
	display: grid;
	max-height: 14rem;
	grid-template-columns: repeat(auto-fill, minmax(min(100%, var(--sticker-cell-min-width)), 1fr));
	align-content: start;
	gap: 0.25rem;
	overflow-y: auto;
	padding: 0.75rem;
}

.sticker-picker__item {
	display: flex;
	min-width: 0;
	align-items: center;
	justify-content: center;
	padding: 0.5rem;
	border-radius: 0.25rem;
	cursor: pointer;
}

.sticker-picker__packs {
	display: flex;
	overflow-x: auto;
	border-top: 1px solid var(--app-border);
}

.sticker-picker__pack {
	display: flex;
	min-width: 3.5rem;
	flex: 1 0 auto;
	align-items: center;
	justify-content: center;
	padding: 0.6rem;
	border-bottom: 3px solid transparent;
	cursor: pointer;

	:deep(.sticker-image) {
		height: 1.5rem;
	}
}

.sticker-picker__pack--active {
	border-bottom-color: var(--app-main);
	background: var(--app-router-hover);
}

.sticker-picker__item:hover,
.sticker-picker__item:focus-visible,
.sticker-picker__pack:hover,
.sticker-picker__close:hover {
	background: var(--app-router-hover);
}

.sticker-picker__state {
	display: flex;
	min-height: 6rem;
	align-items: center;
	justify-content: center;
	gap: 0.75rem;
	color: var(--app-text-muted);
}
</style>
