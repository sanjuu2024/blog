<template>
	<div class="user-profile pb-2 shadow-md">
		<div class="base-info-wrapper">
			<div class="base-info-inner flex content-center items-center">
				<div class="user-avatar">
					<AppUserAvatar
						:avatar-url="userProfile.avatarUrl"
						:name="userProfile.nickname"
						:user-id="userProfile.id"
						:size="80"
						class="mr-4 shadow"
					/>
				</div>
				<div class="user-name-and-bio">
					<div class="mb-2 flex items-center gap-2">
						<p class="text-3xl font-bold text-white">
							{{ userProfile.nickname }}
						</p>
						<i-lucide-chess-queen
							class="text-md text-white"
							v-if="userProfile.role === USER_ROLE.ADMIN"
						/>
					</div>
					<p class="text-sm text-white">
						{{ userProfile.bio ? userProfile.bio : '这个家伙很懒，什么也没有留下' }}
					</p>
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

		<div class="article-list-wrapper">
			<div class="article-list-wrapper-title">
				<component
					:is="getRouteIcon('favorite')"
					class="article-list-wrapper-title__icon"
				/>
				<span>收藏的文章</span>
			</div>
			<div class="article-list-empty">
				<p class="article-list-empty__title">暂无收藏的文章</p>
				<p class="article-list-empty__description">以后收藏过的文章会展示在这里。</p>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import getRouteIcon from '@/utils/getRouteIcon';
import { useUserProfile } from '../composables/useUserProfile';
import { USER_ROLE } from '../types/user';

const { userProfile, getUserProfile } = useUserProfile();
const router = useRouter();

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
	height: 200px;
	padding: 1rem 2rem;
	background: url('@/assets/img/food.png') repeat var(--app-main);
	box-shadow: inset 0 -120px 80px -100px rgba(0, 0, 0, 0.5);
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
</style>
