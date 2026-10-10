import { computed, nextTick, shallowRef, type Ref } from 'vue';
import { useEventListener, useResizeObserver, useTimeoutFn } from '@vueuse/core';

// 列表与通知跳转共用定位，初次加载的高度变化只在短时间内校正，不持续干扰阅读。
export function useArticleCommentAnchor(pageRef: Ref<HTMLElement | null>) {
	const target = shallowRef<HTMLElement | null>(null);
	const { start, stop: stopTimeout } = useTimeoutFn(stop, 10_000, { immediate: false });

	function stop() {
		target.value = null;
		stopTimeout();
	}

	function align() {
		if (!target.value) return;
		if (!target.value.isConnected) {
			stop();
			return;
		}
		const offset = Number.parseFloat(getComputedStyle(target.value).scrollMarginTop) || 0;
		const container = pageRef.value?.closest<HTMLElement>('.app-layout');
		if (container) {
			container.scrollTo({
				top: Math.max(
					0,
					container.scrollTop +
						target.value.getBoundingClientRect().top -
						container.getBoundingClientRect().top -
						offset,
				),
				behavior: 'instant',
			});
		} else {
			target.value.scrollIntoView({ block: 'start', behavior: 'instant' });
		}
	}

	async function locate(element: HTMLElement) {
		stop();
		target.value = element;
		start();
		await nextTick();
		align();
	}

	// 不监听 scroll：程序滚动也会触发它，不能因此提前结束图片加载后的校正。
	useResizeObserver(
		computed(() => (target.value ? pageRef.value : null)),
		align,
	);
	useEventListener(document, ['wheel', 'touchstart', 'pointerdown', 'keydown'], stop, {
		passive: true,
	});

	return { locate, stop };
}
