import { computed, onMounted, ref, shallowRef } from 'vue';
import { getCommentStickers } from '../api/commentStickerApi';
import type { CommentStickerPack } from '../types/sticker';

const packs = shallowRef<CommentStickerPack[]>([]);
const loading = ref(false);
const failed = ref(false);
let loaded = false;
let pending: Promise<void> | null = null;
const stickers = computed(
	() => new Map(packs.value.flatMap((pack) => pack.items.map((item) => [item.token, item]))),
);

async function loadStickers() {
	if (loaded) return;
	if (pending) return pending;
	loading.value = true;
	failed.value = false;
	// 所有评论与回复共享一次目录请求，失败后由选择器显式重试。
	pending = (async () => {
		try {
			packs.value = await getCommentStickers();
			loaded = true;
		} catch {
			failed.value = true;
		} finally {
			loading.value = false;
			pending = null;
		}
	})();
	return pending;
}

export function useCommentStickers() {
	onMounted(() => {
		if (!failed.value) void loadStickers();
	});
	return { packs, stickers, loading, failed, loadStickers };
}
