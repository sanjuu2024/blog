<template>
	<template
		v-for="(part, index) in parts"
		:key="index"
	>
		<StickerImage
			v-if="part.sticker"
			:name="part.sticker.name"
			:url="part.sticker.url"
		/>
		<template v-else>{{ part.text }}</template>
	</template>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useStickers } from '@/composables/useStickers';
import { splitStickerContent } from '@/utils/sticker';
import StickerImage from './StickerImage.vue';

const props = defineProps<{ content: string }>();
const { stickers } = useStickers();
const parts = computed(() => splitStickerContent(props.content, stickers.value));
</script>
