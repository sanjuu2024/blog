<template>
	<main class="friend-links-page">
		<div
			v-if="loadFailed"
			class="friend-links-page__state"
		>
			<p>友链加载失败</p>
			<el-button
				type="primary"
				@click="loadFriendLinks"
			>
				重试
			</el-button>
		</div>
		<AppLoading
			v-else
			:loading="loading"
		>
			<div class="friend-links-page__sections">
				<section
					v-for="section in sections"
					:key="section.section"
					class="friend-links-section"
				>
					<header class="friend-links-section__header">
						<div class="text-2xl">
							<i-streamline-ultimate-color-notes-book
								v-if="section.section === FRIEND_LINK_SECTION.FRIENDS"
								aria-hidden="true"
							/>
							<i-streamline-ultimate-color-vip-crown-queen-1
								v-else-if="section.section === FRIEND_LINK_SECTION.EXPERTS"
								aria-hidden="true"
							/>
							<i-streamline-ultimate-color-browser-com
								v-else-if="section.section === FRIEND_LINK_SECTION.PEERS"
								aria-hidden="true"
							/>
						</div>
						<h2>{{ section.name }}</h2>
					</header>
					<div
						v-if="section.records.length"
						class="friend-links-section__grid"
					>
						<FriendLinkItem
							v-for="friendLink in section.records"
							:key="friendLink.id"
							:friend-link="friendLink"
						/>
					</div>
					<p
						v-else
						class="friend-links-section__empty"
					>
						静待有缘者。
					</p>
				</section>
			</div>
		</AppLoading>

		<section class="friend-links-application">
			<div class="friend-links-application-header">
				<i-lucide-info aria-hidden="true" />
				<h2>申请友链</h2>
			</div>
			<p class="friend-links-application__intro">
				开放友链申请，请至
				<RouterLink
					to="/messages"
					class="friend-links-application__message-link"
					>留言板</RouterLink
				>
				留言。
			</p>
			<h3>申请时请提供以下信息：</h3>
			<ol class="friend-links-application__requirements">
				<li>网站名称</li>
				<li>网站地址（必须使用 HTTPS）</li>
				<li>网站描述</li>
				<li>头像图片或网站代表图地址（可选，建议使用清晰的正方形图片）</li>
			</ol>
			<div
				v-if="siteProfile"
				class="friend-links-application__profile"
			>
				<h3>本站的部分信息：</h3>
				<ul class="friend-links-application__site-info">
					<li><span>名称：</span>{{ siteProfile.name }}</li>
					<li>
						<span>地址：</span>
						<a
							:href="siteProfile.url"
							target="_blank"
							rel="noopener noreferrer"
							>{{ siteProfile.url }}</a
						>
					</li>
					<li><span>描述：</span>{{ siteProfile.description }}</li>
					<li>
						<div class="friend-links-application__logo-row">
							<div>
								<span>头像图片或网站代表图：</span>
								<a
									:href="siteProfile.logoUrl"
									target="_blank"
									rel="noopener noreferrer"
									>{{ siteProfile.logoUrl }}</a
								>
							</div>
							<a
								:href="siteProfile.logoUrl"
								target="_blank"
								rel="noopener noreferrer"
								class="friend-links-application__logo-link"
								:aria-label="`${siteProfile.name} 头像图片或网站代表图`"
							>
								<AppImage
									:url="siteProfile.logoUrl"
									:alt="siteProfile.name"
									class="friend-links-application__logo"
								/>
							</a>
						</div>
					</li>
				</ul>
			</div>
		</section>
	</main>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import AppLoading from '@/components/AppLoading.vue';
import AppImage from '@/components/AppImage.vue';
import { FRIEND_LINK_SECTION } from '../types/friendLink';
import FriendLinkItem from '../components/FriendLinkItem.vue';
import { usePublicFriendLinks } from '../composables/usePublicFriendLinks';

defineOptions({ name: 'LinkPage' });

const { sections, siteProfile, loading, loadFailed, loadFriendLinks } = usePublicFriendLinks();

onMounted(loadFriendLinks);
</script>

<style scoped lang="scss">
.friend-links-page {
	width: min(100%, var(--app-about-content-width));
	margin: 0 auto;
}

.friend-links-page__state {
	display: flex;
	min-height: 10rem;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 0.75rem;
	color: var(--app-text-muted);
}

.friend-links-page__sections {
	display: grid;
	gap: 1.75rem;
}

.friend-links-section {
	min-width: 0;
	margin-bottom: 1rem;

	.friend-links-section__header {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding-bottom: 0.5rem;
		margin-bottom: 1rem;

		h2 {
			font-size: 1.75rem;
			font-weight: bold;
		}
	}

	.friend-links-section__grid {
		display: grid;
		grid-template-columns: repeat(5, minmax(0, 1fr));
		gap: 1rem;

		@media (width < 768px) {
			grid-template-columns: repeat(4, minmax(0, 1fr));
		}

		@media (width < 600px) {
			grid-template-columns: repeat(3, minmax(0, 1fr));
		}

		@media (width < 480px) {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}

		@media (width < 300px) {
			grid-template-columns: minmax(0, 1fr);
		}
	}

	.friend-links-section__empty {
		color: var(--app-text-muted);
	}
}

.friend-links-application {
	margin-top: 2.25rem;
	padding-top: 1rem;
	border-top: 1px solid var(--app-border);
	padding-bottom: 3rem;

	.friend-links-application-header {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		margin-block: 1rem;
		font-size: 1.5rem;
		font-weight: bold;
	}

	.friend-links-application__intro {
		font-size: 1.1rem;
		line-height: 1.8;
	}

	.friend-links-application__message-link {
		border-bottom: 1px solid currentcolor;
		color: var(--app-text);
		text-decoration: none;
		transition:
			color 0.15s ease,
			background-color 0.15s ease;

		&:hover,
		&:active {
			color: var(--app-main);
		}
	}

	h3 {
		margin-block: 1rem 0.5rem;
		font-size: 1.1rem;
		font-weight: bold;
	}

	.friend-links-application__requirements,
	.friend-links-application__site-info {
		margin: 0;
		padding-left: 1.5rem;
		font-size: 1rem;
		line-height: 2;
		list-style: disc;

		li {
			min-width: 0;
			overflow-wrap: anywhere;
		}
	}

	.friend-links-application__profile {
		margin-top: 1rem;
	}

	.friend-links-application__logo-row {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.friend-links-application__logo-link {
		width: 1.1em;
		height: 1.1em;
		line-height: 0;
	}

	.friend-links-application__logo {
		width: 100%;
		height: 100%;
	}
}
</style>
