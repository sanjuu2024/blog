<template>
	<div>
		<!-- 顶部 -->
		<div class="home-header">
			<!-- 背景图片 -->
			<AppImage
				:url="homeBgUrl"
				alt="首页背景图片"
				class="home-header__bg"
			/>
			<!-- 图片上的暗色遮罩 -->
			<div class="absolute inset-0 bg-black/20"></div>

			<!-- 前景内容 -->
			<div class="home-header__content">
				<!-- logo -->
				<img
					:src="logoText"
					alt="青禾边"
					class="home-header__logo"
				/>

				<!-- 副标题文案 -->
				<p class="home-header__subtitle">
					青禾向野，晚风在肩，<br />岁岁安然，皆在青禾边。
				</p>
			</div>
		</div>

		<!-- 置顶文章 -->
		<section class="home-section home-top">
			<div class="home-section__title">
				<i-solar-pin-outline class="home-section__title-icon" />
				<p>置顶文章</p>
			</div>
			<AppLoading
				:loading="loadingTopArticles"
				class="top-article-list"
			>
				<div
					v-if="topArticlesLoadFailed"
					class="home-section-state"
				>
					<p>置顶文章加载失败</p>
					<el-button
						type="primary"
						@click="getTopArticles"
					>
						重试
					</el-button>
				</div>
				<p
					v-else-if="topArticlesLoaded && !topArticles.length"
					class="home-section-state"
				>
					暂无置顶文章
				</p>
				<template v-else>
					<TopArticleCard
						v-for="article in topArticles"
						:key="article.id"
						:article="article"
					/>
				</template>
			</AppLoading>
		</section>

		<!-- 最新文章 -->
		<section class="home-section home-latest">
			<div class="home-section__title">
				<i-solar-clock-circle-outline class="home-section__title-icon" />
				<p>最新文章</p>
			</div>
			<AppLoading
				:loading="loadingLatestArticles"
				class="latest-article-list"
			>
				<div
					v-if="latestArticlesLoadFailed"
					class="home-section-state"
				>
					<p>最新文章加载失败</p>
					<el-button
						type="primary"
						@click="getLatestArticles"
					>
						重试
					</el-button>
				</div>
				<p
					v-else-if="latestArticlesLoaded && !latestArticles.length"
					class="home-section-state"
				>
					暂无最新文章
				</p>
				<template v-else>
					<LatestArticleListItem
						v-for="article in latestArticles"
						:key="article.id"
						:article="article"
					/>
				</template>
			</AppLoading>
		</section>

		<!-- 一级分类 -->
		<section class="home-section home-category">
			<div class="home-section__title">
				<i-solar-folder-outline class="home-section__title-icon" />
				<p>分类归档</p>
			</div>
			<AppLoading
				:loading="loadingCategories"
				class="category-list"
			>
				<div
					v-if="categoriesLoadFailed"
					class="home-section-state"
				>
					<p>分类归档加载失败</p>
					<el-button
						type="primary"
						@click="getEnabledCategoryList"
					>
						重试
					</el-button>
				</div>
				<p
					v-else-if="categoriesLoaded && !enabledCategoryList.length"
					class="home-section-state"
				>
					暂无分类
				</p>
				<template v-else>
					<CategoryCard
						v-for="(category, index) in enabledCategoryList"
						:key="category.id"
						:category="category"
						:number="index + 1"
					/>
				</template>
			</AppLoading>
		</section>

		<!-- 标签云 -->
		<section class="home-section home-tags">
			<div class="home-section__title">
				<i-solar-tag-horizontal-outline class="home-section__title-icon" />
				<p>标签云</p>
			</div>
			<AppLoading
				:loading="loadingTags"
				class="tags-cloud"
			>
				<div
					v-if="tagsLoadFailed"
					class="home-section-state"
				>
					<p>标签云加载失败</p>
					<el-button
						type="primary"
						@click="getEnabledTagList"
					>
						重试
					</el-button>
				</div>
				<p
					v-else-if="tagsLoaded && !enabledTagList.length"
					class="home-section-state"
				>
					暂无标签
				</p>
				<VueWordCloud
					v-else
					style="width: 100%; height: 300px"
					:words="tagCloudWords"
					:spacing="0.3"
				>
					<template v-slot="{ text }">
						<div
							:style="{
								color: getColor(text),
							}"
						>
							{{ text }}
						</div>
					</template>
				</VueWordCloud>
			</AppLoading>
		</section>
	</div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue';
import logoText from '@/assets/svg/logo-text.svg';
import { useHomePage } from '../composables/useHomePage';
import { useEnabledCategoryList } from '@/modules/category/composables/useEnabledCategoryList.ts';
import { useEnabledTagList } from '@/modules/tag/composables/useEnabledTagList.ts';
import TopArticleCard from '../components/TopArticleCard.vue';
import LatestArticleListItem from '../components/LatestArticleListItem.vue';
import CategoryCard from '../components/CategoryCard.vue';
import VueWordCloud from 'vuewordcloud';

defineOptions({
	name: 'HomePage',
});

const {
	loadingTopArticles,
	topArticlesLoadFailed,
	topArticlesLoaded,
	topArticles,
	loadingLatestArticles,
	latestArticlesLoadFailed,
	latestArticlesLoaded,
	latestArticles,
	getTopArticles,
	getLatestArticles,
} = useHomePage();

const {
	loading: loadingCategories,
	loadFailed: categoriesLoadFailed,
	loaded: categoriesLoaded,
	enabledCategoryList,
	getEnabledCategoryList,
} = useEnabledCategoryList();

const {
	loading: loadingTags,
	loadFailed: tagsLoadFailed,
	loaded: tagsLoaded,
	enabledTagList,
	getEnabledTagList,
} = useEnabledTagList();

onMounted(() => {
	getTopArticles();
	getLatestArticles();
	getEnabledCategoryList();
	getEnabledTagList();
});

const homeBgUrls = [
	'/img/home-bg0.jpg',
	'/img/home-bg1.jpg',
	'/img/home-bg2.jpg',
	'/img/home-bg3.jpg',
	'/img/home-bg4.jpg',
	'/img/home-bg5.jpg',
	'/img/home-bg6.jpg',
] as const;

// const homeBgUrl = homeBgUrls[Math.floor(Math.random() * homeBgUrls.length)]; // 测试用
const homeBgUrl = homeBgUrls[new Date().getDay() % homeBgUrls.length]; // 根据星期几来显示背景图

const tagCloudWords = computed(() => {
	return enabledTagList.value.map((tag) => [tag.name, tag.articleCount]);
});

const colorList = ['#74482A', '#D1B022', 'var(--app-main)']; //  'Indigo' 也不错，不过不算同个氛围
const getColor = (str: string) => {
	let hash = 0;
	for (let i = 0; i < str.length; i++) {
		hash = str.charCodeAt(i) + ((hash << 5) - hash);
	}
	const index = Math.abs(hash) % colorList.length;
	return colorList[index];
};
</script>

<style lang="scss" scoped>
.home-header {
	position: relative;
	height: 400px;
	width: 100%;
	overflow: hidden;
	border: 1px solid var(--app-border);
	// border-radius: 0.7rem;

	.home-header__bg {
		position: absolute;
		height: 100%;
		width: 100%;
		inset: 0;
	}

	.home-header__content {
		position: absolute;
		inset: 0;
		z-index: 10;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		color: white;

		.home-header__logo {
			height: 110px;
			filter: brightness(0) invert(1) drop-shadow(0 2px 3px rgb(0 0 0 / 75%));
		}

		.home-header__subtitle {
			margin-top: 1.5rem;
			font-size: 1.2rem;
			font-weight: 580;
			text-align: center;
			text-shadow: 2px 2px 3px rgba(0, 0, 0, 0.7);
		}
	}
}

.home-section {
	padding-top: 4rem;

	.home-section-state {
		grid-column: 1 / -1; // 当父容器是 grid 布局时，子元素占满整行；在非 Grid 容器中，grid-column 不会产生作用。
		min-height: 10rem;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 1rem;
		color: var(--app-text-muted);
		text-align: center;
	}

	.home-section__title {
		font-size: 1.5rem;
		font-weight: 440;
		margin-bottom: 2rem;
		display: flex;
		gap: 0.7rem;
		align-items: center;
		box-sizing: border-box;
		border-left: 4px solid var(--app-main);
		padding-left: 1rem;

		.home-section__title-icon {
			font-size: 1.2rem;
			color: var(--app-main);
		}
	}
}

.top-article-list {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 1rem;
}

.latest-article-list {
	display: flex;
	flex-direction: column;

	.latest-article-list-item {
		border-bottom: 1px solid var(--app-border);

		&:last-child {
			border-bottom: none;
		}
	}
}

.category-list {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 1rem;
}

@media (max-width: 767px) {
	.top-article-list {
		grid-template-columns: 1fr;
	}

	.category-list {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
}

@media (max-width: 480px) {
	.category-list {
		grid-template-columns: 1fr;
	}
}
</style>
