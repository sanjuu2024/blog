<template>
	<div class="user-profile pb-2 shadow-md">
		<div class="base-info-wrapper">
			<div class="base-info-inner flex w-full justify-between">
				<div class="base-info">
					<div class="user-avatar">
						<AppUserAvatar
							:avatar-url="userProfile.avatarUrl"
							:name="userProfile.nickname"
							:user-id="userProfile.id"
							:size="isMobile ? 60 : 80"
							class="mr-4 shadow"
						/>
					</div>
					<div class="user-name-and-bio">
						<div class="mb-2 flex items-center gap-2">
							<p class="nickname text-3xl font-bold text-white">
								{{ userProfile.nickname }}
							</p>
							<i-lucide-chess-queen
								class="text-md text-white"
								v-if="userProfile.role === USER_ROLE.ADMIN"
							/>
						</div>
						<p
							ref="bioRef"
							id="user-profile-bio"
							class="user-profile__bio text-sm text-white"
							:class="{ 'user-profile__bio--expanded': bioExpanded }"
						>
							{{ userProfile.bio ? userProfile.bio : '这个家伙很懒，什么也没有留下' }}
						</p>
						<button
							v-if="bioOverflows"
							type="button"
							class="user-profile__bio-toggle"
							aria-controls="user-profile-bio"
							:aria-expanded="bioExpanded"
							@click="toggleBio"
						>
							{{ bioExpanded ? '收起' : '展开' }}
						</button>
					</div>
				</div>
				<div class="edit-button">
					<el-button
						type="warning"
						class="mb-1"
						@click="router.push('/users/me/settings')"
					>
						设置
						<template #icon>
							<i-lucide-settings />
						</template>
					</el-button>
				</div>
			</div>
		</div>

		<div class="article-list-wrapper">
			<div class="article-list-wrapper-title">
				<component
					:is="getRouteIcon('like')"
					class="article-list-wrapper-title__icon"
				/>
				<span>点赞的文章</span>
			</div>
			<div class="article-list-empty">
				<p class="article-list-empty__title">暂无点赞的文章</p>
				<p class="article-list-empty__description">以后点赞过的文章会展示在这里。</p>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useMediaQuery, useResizeObserver } from '@vueuse/core';
import getRouteIcon from '@/utils/getRouteIcon';
import { useUserProfile } from '../composables/useUserProfile';
import { USER_ROLE } from '../types/user';

const isMobile = useMediaQuery('(width < 400px)');
const bioRef = ref<HTMLElement | null>(null);
const bioExpanded = ref(false);
const bioOverflows = ref(false);
const OVERFLOW_TOLERANCE_PX = 1;

const { userProfile, getUserProfile } = useUserProfile();
const router = useRouter();

function updateBioOverflow() {
	if (!bioRef.value || bioExpanded.value) return;
	bioOverflows.value =
		bioRef.value.scrollHeight - bioRef.value.clientHeight > OVERFLOW_TOLERANCE_PX;
}

async function toggleBio() {
	bioExpanded.value = !bioExpanded.value;
	await nextTick();
	if (!bioExpanded.value) {
		updateBioOverflow();
	}
}

useResizeObserver(bioRef, updateBioOverflow);

watch(
	() => userProfile.bio,
	async () => {
		bioExpanded.value = false;
		await nextTick();
		updateBioOverflow();
	},
);

defineOptions({
	name: 'UserProfilePage',
});

onMounted(() => {
	getUserProfile();
});
</script>

<style lang="scss" scoped>
.base-info-wrapper {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	width: 100%;
	min-height: 200px;
	padding: 1rem 2rem;
	background: url('@/assets/img/food.png') repeat var(--app-main);
	box-shadow: inset 0 -120px 80px -100px rgb(0 0 0 / 50%);
}

.article-list-wrapper {
	width: 100%;
	margin-block: 1rem;
	padding-inline: 1rem;

	.article-list-wrapper-title {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		margin-bottom: 1rem;
		font-weight: bold;
	}
}

.user-name-and-bio {
	min-width: 0;
}

.nickname {
	display: -webkit-box;
	overflow: hidden;
	overflow-wrap: anywhere;
	white-space: pre-wrap;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	line-clamp: 2;
}

.user-profile__bio {
	display: -webkit-box;
	overflow: hidden;
	overflow-wrap: anywhere;
	white-space: pre-wrap;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	line-clamp: 2;
}

.user-profile__bio--expanded {
	display: block;
	-webkit-line-clamp: unset;
	line-clamp: unset;
}

.user-profile__bio-toggle {
	margin-top: 0.25rem;
	border: 0;
	background: transparent;
	color: white;
	cursor: pointer;
	font-size: 0.8rem;
	font-weight: bold;
	text-decoration: underline;
}

.user-profile__bio-toggle:hover,
.user-profile__bio-toggle:focus-visible {
	opacity: 0.8;
}

.article-list-empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	min-height: 120px;
	border: 1px dashed var(--app-border);
	border-radius: 0.75rem;
	color: var(--app-text-muted);
	text-align: center;

	.article-list-empty__title {
		margin-bottom: 0.25rem;
		color: var(--app-text);
		font-weight: bold;
	}

	.article-list-empty__description {
		font-size: 0.9rem;
	}
}

.edit-button {
	margin-top: auto;
	margin-left: auto;
}

@media (width < 400px) {
	.base-info-inner {
		flex-direction: column;
		align-items: stretch;
		gap: 0.5rem;
	}

	.base-info {
		display: flex;
		align-items: center;

		.nickname {
			font-size: x-large;
		}
	}

	.edit-button {
		width: 100%;

		:deep(.el-button) {
			width: 100%;
		}
	}
}
</style>
