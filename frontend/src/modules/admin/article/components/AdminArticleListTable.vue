<template>
	<div class="admin-article-table">
		<!-- 文章列表 -->
		<el-table
			:data="articleList"
			row-key="id"
			class="admin-article-table"
		>
			<el-table-column
				align="center"
				width="30"
				prop="isTop"
			>
				<template #default="{ row }">
					<div class="admin-article-is-top">
						<i-solar-pin-bold
							v-if="row.isTop"
							style="color: var(--app-button-bg)"
						/>
						<i-solar-pin-outline v-else />
					</div>
				</template>
			</el-table-column>
			<el-table-column
				align="center"
				label="总览"
			>
				<template #default="{ row }: { row: AdminArticleListItem }">
					<div class="flex items-center">
						<router-link
							:to="`/admin/articles/${row.id}/edit`"
							class="flex items-center font-bold"
						>
							<div
								class="relative h-16 w-16 shrink-0 overflow-hidden rounded border border-gray-200"
							>
								<!-- 加载中 -->
								<div
									v-if="
										row.coverUrl &&
										!coverLoadedArticleIds.has(row.id) &&
										!coverLoadFailedArticleIds.has(row.id)
									"
									class="absolute inset-0 flex h-full w-full items-center justify-center bg-gray-100 text-gray-300"
								>
									<!-- tailwind css 自带的 animate-spin -->
									<i-lucide-loader class="animate-spin text-xl" />
								</div>

								<!-- 加载成功 -->
								<img
									v-if="row.coverUrl && !coverLoadFailedArticleIds.has(row.id)"
									:src="row.coverUrl"
									alt="文章封面"
									class="absolute inset-0 h-full w-full rounded object-cover"
									:class="{ 'opacity-0': !coverLoadedArticleIds.has(row.id) }"
									@load="markCoverLoaded(row.id)"
									@error="markCoverFailed(row.id)"
								/>

								<!-- 无封面 -->
								<div
									v-else-if="!row.coverUrl"
									class="absolute inset-0 flex h-full w-full items-center justify-center rounded bg-gray-100 text-gray-400"
								>
									<i-lucide-image class="text-xl" />
								</div>

								<!-- 加载失败 -->
								<div
									v-else
									class="absolute inset-0 flex h-full w-full items-center justify-center rounded bg-gray-100 text-gray-400"
								>
									<i-lucide-image-off class="text-xl" />
								</div>
							</div>

							<span class="mx-4 text-left">{{ row.title }}</span>
						</router-link>
					</div>
				</template>
			</el-table-column>

			<el-table-column
				align="center"
				width="100"
				label="分类"
			>
				<template #default="{ row }: { row: AdminArticleListItem }">
					{{ row.category.parent.name }} - {{ row.category.name }}
				</template>
			</el-table-column>

			<el-table-column
				label="状态"
				align="center"
				width="100"
			>
				<template #default="{ row }: { row: AdminArticleListItem }">
					<div
						v-if="row.status == ARTICLE_STATUS.DRAFT"
						class="text-center"
					>
						<div class="flex items-center justify-center">
							<i-lets-icons-edit class="text-xl text-yellow-600" />
						</div>
						<p>草稿</p>
					</div>
					<div
						v-else-if="row.status == ARTICLE_STATUS.PUBLISHED"
						class="text-center"
					>
						<div class="flex items-center justify-center">
							<i-lets-icons-file-dock class="text-xl text-green-600" />
						</div>
						<p>已发布</p>
					</div>
					<div
						v-else
						class="text-center"
					>
						<div class="flex items-center justify-center">
							<i-lets-icons-lock class="text-xl text-gray-400" />
						</div>
						<p>下线</p>
					</div>
				</template>
			</el-table-column>

			<el-table-column
				align="center"
				width="180"
				label="发表时间"
			>
				<template #default="{ row }: { row: AdminArticleListItem }">
					{{ row.publishedAt ? formatDateTime(row.publishedAt) : '-' }}
				</template>
			</el-table-column>

			<el-table-column
				align="center"
				width="180"
				label="最近编辑时间"
			>
				<template #default="{ row }: { row: AdminArticleListItem }">
					{{ row.updatedAt ? formatDateTime(row.updatedAt) : '-' }}
				</template>
			</el-table-column>

			<el-table-column
				label="文章操作"
				align="center"
				width="320"
				fixed="right"
			>
				<!-- 已发布状态可以改为下线状态，其他状态可以改为已发布状态 -->
				<template #default="{ row }: { row: AdminArticleListItem }">
					<el-button
						type="warning"
						title="编辑文章"
						@click="router.push(`/admin/articles/${row.id}/edit`)"
					>
						<template #icon>
							<i-ep-edit />
						</template>
						编辑
					</el-button>

					<el-popconfirm
						:hide-icon="true"
						:hide-after="0"
						:title="`确认${row.status === ARTICLE_STATUS.DRAFT ? '发布' : row.status === ARTICLE_STATUS.PUBLISHED ? '下线' : '重新发布'} ${row.title}？`"
						@confirm="emit('updateStatus', row)"
					>
						<template #reference>
							<el-button
								:type="
									row.status === ARTICLE_STATUS.DRAFT
										? 'success'
										: row.status === ARTICLE_STATUS.PUBLISHED
											? 'danger'
											: 'warning'
								"
								:title="
									row.status === ARTICLE_STATUS.DRAFT
										? '发布'
										: row.status === ARTICLE_STATUS.PUBLISHED
											? '下线'
											: '重新发布'
								"
							>
								<template #icon>
									<i-ep-upload v-if="row.status === ARTICLE_STATUS.DRAFT" />
									<i-ep-download
										v-else-if="row.status === ARTICLE_STATUS.PUBLISHED"
									/>
									<i-ep-refresh v-else />
								</template>
								{{
									row.status === ARTICLE_STATUS.DRAFT
										? '发布'
										: row.status === ARTICLE_STATUS.PUBLISHED
											? '下线'
											: '重新发布'
								}}
							</el-button>
						</template>
					</el-popconfirm>

					<!-- @confirm="clickDeleteArticle(row.id)" -->
					<el-popconfirm
						:title="`确认删除文章 ${row.title} 吗？`"
						@confirm="emit('delete', row.id)"
					>
						<template #reference>
							<el-button
								type="danger"
								title="删除文章"
							>
								<template #icon>
									<i-ep-delete />
								</template>
								删除
							</el-button>
						</template>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<!-- 分页部分 -->
		<!-- 注意 current-page 和 page-size 是 v-model 双向绑定，而不只是 v-bind -->
		<el-pagination
			v-model:current-page="pageNum"
			v-model:page-size="pageSize"
			:background="true"
			layout="prev, pager, next, jumper, ->, sizes, total"
			:total="pageMeta.total"
			:page-sizes="[3, 5, 7, 9]"
			@current-change="emit('pageChange')"
			@size-change="emit('pageChange')"
			class="mt-6"
		/>
	</div>
</template>

<script setup lang="ts">
import { reactive, onActivated } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import { useRouter } from 'vue-router';
import {
	ARTICLE_STATUS,
	type AdminArticleListItem,
	type AdminArticlePageMeta,
} from '../types/adminArticle';

const router = useRouter();

// 封面图片加载失败的文章 ID 集合，用于判断是否需要显示图片加载失败占位图
const coverLoadFailedArticleIds = reactive(new Set<number>());

function markCoverFailed(articleId: number) {
	coverLoadFailedArticleIds.add(articleId);
}

// 封面图片加载好了的文章 ID 集合，用于判断是否需要显示图片加载中占位图
const coverLoadedArticleIds = reactive(new Set<number>());
function markCoverLoaded(articleId: number) {
	coverLoadedArticleIds.add(articleId);
}

defineOptions({
	name: 'AdminArticleListTable',
});

// 注意 pageNum 和 pageSize 是通过 v-model 双向绑定的，所以用 defineModel，而不是 defineProps
const pageNum = defineModel<number>('pageNum', { required: true });
const pageSize = defineModel<number>('pageSize', { required: true });

defineProps<{
	articleList: AdminArticleListItem[];
	pageMeta: AdminArticlePageMeta;
}>();

const emit = defineEmits<{
	delete: [number];
	updateStatus: [AdminArticleListItem];
	pageChange: [];
}>();

onActivated(() => {
	// 组件激活时重置封面加载失败的 ID 集合，重新尝试加载封面图片
	coverLoadFailedArticleIds.clear();
	// 并且重新拉取可能更新后的文章列表数据
	emit('pageChange');
});
</script>

<style scoped lang="scss">
// 自定义表格样式，覆盖 Element Plus 默认的行 hover 和斑马纹背景色
.admin-article-table {
	--el-table-row-hover-bg-color: #eef6f0;
}

.admin-article-table :deep(.el-table__body tr.el-table__row--striped td.el-table__cell) {
	background: #f7faf8;
}
</style>
