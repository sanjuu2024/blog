<template>
	<el-popover
		v-if="!disabled"
		placement="top"
		:width="280"
		:trigger="['hover', 'focus']"
		:show-after="100"
		:hide-after="50"
		@before-enter="loadProfile"
		@after-enter="updateBioOverflow"
		@hide="bioExpanded = false"
	>
		<!-- 使用 #reference 放置触发元素 -->
		<template #reference>
			<span
				class="public-user-profile-popover__reference"
				role="button"
				tabindex="0"
				aria-label="查看用户公开资料"
			>
				<!-- 调用本组件的时候，传入插槽中的内容就是常驻在页面上、可以触发该 popover 的元素（即用户悬浮什么内容时打开弹层） -->
				<slot></slot>
			</span>
		</template>

		<div class="public-user-profile-popover__content">
			<div
				v-if="loading"
				class="public-user-profile-popover__state"
			>
				<i-lucide-loader class="animate-spin text-xl text-gray-400" />
			</div>

			<div
				v-else-if="loadFailed"
				class="public-user-profile-popover__state"
			>
				<span>公开资料加载失败</span>
				<el-button
					link
					type="primary"
					@click="retry"
				>
					重试
				</el-button>
			</div>

			<template v-else-if="profile">
				<div class="public-user-profile-popover__header">
					<AppUserAvatar
						:avatar-url="profile.avatarUrl"
						:name="profile.nickname || profile.username"
						:user-id="profile.id"
						:deleted="profile.deleted"
						:is-admin="profile.role === 'ADMIN'"
						:size="48"
					/>
					<div class="public-user-profile-popover__identity">
						<strong
							:class="{
								'public-user-profile-popover__deleted-name': profile.deleted,
							}"
						>
							{{ profile.nickname || profile.username }}
						</strong>
						<span v-if="!profile.deleted">@{{ profile.username }}</span>
					</div>
				</div>
				<p
					v-if="!profile.deleted"
					ref="bioRef"
					class="public-user-profile-popover__bio"
					:class="{ 'public-user-profile-popover__bio--expanded': bioExpanded }"
				>
					{{ profile.bio?.trim() || '这个家伙很懒，什么也没有留下' }}
				</p>
				<button
					v-if="!profile.deleted && bioOverflows"
					type="button"
					class="public-user-profile-popover__bio-toggle"
					:aria-expanded="bioExpanded"
					@click.stop="toggleBio"
				>
					{{ bioExpanded ? '收起' : '展开' }}
				</button>
			</template>
		</div>
	</el-popover>
	<span
		v-else
		class="public-user-profile-popover__reference public-user-profile-popover__reference--disabled"
	>
		<slot></slot>
	</span>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { useResizeObserver } from '@vueuse/core';
import AppUserAvatar from './AppUserAvatar.vue';
import { getPublicUserProfile } from '@/modules/user/api/userApi';
import type { PublicUserProfileData } from '@/modules/user/types/user';

defineOptions({
	name: 'PublicUserProfilePopover',
});

const props = defineProps<{
	userId: number;
	disabled?: boolean;
}>();

const profile = ref<PublicUserProfileData | null>(null);
const loading = ref(false);
const loadFailed = ref(false);
const bioRef = ref<HTMLElement | null>(null);
const bioExpanded = ref(false);
const bioOverflows = ref(false);
const OVERFLOW_TOLERANCE_PX = 1;

// 与个人中心一致，仅在真实溢出三行时提供操作；弹层显示后再测量，避免隐藏时的尺寸影响判断。
function updateBioOverflow() {
	if (!bioRef.value || bioExpanded.value) return;
	bioOverflows.value =
		bioRef.value.scrollHeight - bioRef.value.clientHeight > OVERFLOW_TOLERANCE_PX;
}

async function toggleBio() {
	bioExpanded.value = !bioExpanded.value;
	await nextTick();
	if (!bioExpanded.value) updateBioOverflow();
}

useResizeObserver(bioRef, updateBioOverflow);

watch(
	() => profile.value?.bio,
	async () => {
		bioExpanded.value = false;
		bioOverflows.value = false;
		await nextTick();
		updateBioOverflow();
	},
);

// 首次打开时再获取资料，避免文章详情初始化时产生额外请求。
// p.s. userId 可能在请求完成前变化，旧请求不得覆盖新用户的状态。
// 关于该函数内更新 loadFailed 和 loading 前的额外判断：是为了防止异步请求发生竞态（虽然很稀少）
async function loadProfile() {
	if (profile.value || loading.value || loadFailed.value) return;

	loading.value = true;
	const userId = props.userId;
	try {
		const data = await getPublicUserProfile(userId, {
			meta: { showError: false },
		});
		if (props.userId === userId) {
			profile.value = data;
		}
	} catch {
		if (props.userId === userId) {
			loadFailed.value = true;
		}
	} finally {
		if (props.userId === userId) {
			loading.value = false;
		}
	}
}

function retry() {
	loadFailed.value = false;
	void loadProfile();
}

watch(
	() => props.userId,
	() => {
		profile.value = null;
		loading.value = false;
		loadFailed.value = false;
	},
);
</script>

<style scoped lang="scss">
.public-user-profile-popover__reference {
	display: inline-flex;
	align-items: center;
	cursor: pointer;

	&:focus-visible {
		outline: 2px solid var(--app-main);
		outline-offset: 2px;
	}
}

.public-user-profile-popover__reference--disabled {
	cursor: default;
}

.public-user-profile-popover__content {
	min-height: 5rem;
	max-height: min(24rem, calc(100dvh - 2rem));
	overflow-y: auto;
	overscroll-behavior: contain;
}

.public-user-profile-popover__state {
	min-height: 5rem;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 0.5rem;
	color: var(--app-text-muted);
}

.public-user-profile-popover__header {
	display: flex;
	align-items: center;
	gap: 0.75rem;
}

.public-user-profile-popover__identity {
	min-width: 0;
	display: flex;
	flex-direction: column;

	strong,
	span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	span {
		color: var(--app-text-muted);
		font-size: 0.85rem;
	}
}

.public-user-profile-popover__bio {
	display: -webkit-box;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 3;
	line-clamp: 3;
	margin: 0.75rem 0 0;
	color: var(--app-text-muted);
	line-height: 1.6;
	white-space: pre-wrap;
	overflow-wrap: anywhere;
}

.public-user-profile-popover__bio--expanded {
	display: block;
	-webkit-line-clamp: unset;
	line-clamp: unset;
}

.public-user-profile-popover__bio-toggle {
	margin-top: 0.25rem;
	border: 0;
	background: transparent;
	color: var(--app-main);
	cursor: pointer;
	font-size: 0.8rem;
	font-weight: bold;
	text-decoration: underline;
}

.public-user-profile-popover__bio-toggle:hover,
.public-user-profile-popover__bio-toggle:focus-visible {
	opacity: 0.8;
}

.public-user-profile-popover__deleted-name {
	color: var(--app-text-muted-more);
}
</style>
