import { nextTick, ref } from 'vue';
import { until } from '@vueuse/core';

export const articleListScrollPending = ref(false);
const readyListPath = ref<string | null>(null);
const canRestoreSavedPosition = ref(false);
const pageViewPending = ref(false);
const navigationVersion = ref(0);

// 每次成功导航创建新的恢复任务，已取消页面的异步回调不能滚动后续页面。
export function prepareArticleListScroll(pending: boolean) {
	navigationVersion.value++;
	readyListPath.value = null;
	canRestoreSavedPosition.value = false;
	articleListScrollPending.value = pending;
}

export function markArticleListReady(fullPath: string, reuseLoadedList = true) {
	readyListPath.value = fullPath;
	canRestoreSavedPosition.value = reuseLoadedList;
}

export function setPageViewPending(pending: boolean) {
	pageViewPending.value = pending;
}

export async function restoreArticleListScroll(
	fullPath: string,
	position: { left: number; top: number },
): Promise<false> {
	const version = navigationVersion.value;
	await until(
		() =>
			version !== navigationVersion.value ||
			(readyListPath.value === fullPath && !pageViewPending.value),
	).toBe(true);
	await nextTick();
	if (version !== navigationVersion.value) return false;

	// enter 时列表已插入 DOM，在淡入前定位，避免 after-enter 导致显示顶部后再跳转。
	window.scrollTo({
		...(canRestoreSavedPosition.value ? position : { left: 0, top: 0 }),
		behavior: 'instant',
	});
	articleListScrollPending.value = false;
	return false;
}
