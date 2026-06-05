<template>
	<div class="admin-article-edit">
		<el-card>
			<template #header>
				<router-link
					to="/admin/articles"
					class="admin-article-edit-header flex items-center"
				>
					<i-ep-back />
					<span class="ml-2">返回文章列表页</span>
				</router-link>
			</template>
			<!-- 文章元信息 -->
			<el-form
				:model="upsertRequest"
				:rules="rules"
				label-position="right"
				label-width="auto"
				@submit.prevent
				class="mt-4 ml-2"
				ref="theFormRef"
			>
				<el-form-item
					label="文章标题"
					prop="title"
				>
					<el-input
						v-model="upsertRequest.title"
						placeholder="请输入文章标题"
					/>
				</el-form-item>

				<el-form-item
					label="文章封面"
					prop="coverUrl"
				>
					<el-input
						v-model="upsertRequest.coverUrl"
						placeholder="请输入文章封面链接"
					/>
					<div class="preview-cover">
						<img
							v-if="upsertRequest.coverUrl"
							:src="upsertRequest.coverUrl"
							alt="文章封面预览"
							class="mt-4 h-48 w-48 rounded object-cover"
						/>
					</div>
				</el-form-item>

				<el-form-item
					label="文章摘要"
					prop="summary"
				>
					<el-input
						v-model="upsertRequest.summary"
						type="textarea"
						placeholder="请输入文章摘要"
						:rows="3"
					/>
				</el-form-item>

				<el-form-item
					label="文章分类"
					prop="categoryId"
				>
					<el-tree-select
						v-model="upsertRequest.categoryId"
						value-key="id"
						:data="
							categoryList.map((category) => ({
								...category,
								disabled: true,
							}))
						"
						:props="{
							label: 'name',
							disabled: 'disabled',
						}"
						check-strictly
						placeholder="请选择二级分类"
						clearable
						default-expand-all
					/>
				</el-form-item>

				<el-form-item
					label="文章标签"
					prop="tagIds"
					class="flex items-center justify-center"
				>
					<el-tag
						v-for="tagId in upsertRequest.tagIds"
						:key="tagId"
						class="admin-article-tag m-2"
						size="large"
					>
						{{ tagList.find((tag) => tag.id === tagId)?.name || '未知标签' }}
					</el-tag>
					<el-button
						type="primary"
						@click="showTagDialog"
						class="ml-2"
						>选择标签</el-button
					>
				</el-form-item>

				<el-form-item
					label="文章状态"
					prop="status"
				>
					<el-select
						v-model="upsertRequest.status"
						placeholder="请选择文章状态"
					>
						<el-option
							label="草稿"
							:value="ARTICLE_STATUS.DRAFT"
							:disabled="
								articleDetail?.status === ARTICLE_STATUS.PUBLISHED ||
								articleDetail?.status === ARTICLE_STATUS.OFFLINE
							"
						/>
						<el-option
							label="发布"
							:value="ARTICLE_STATUS.PUBLISHED"
							:disabled="false"
						/>
						<el-option
							label="下线"
							:value="ARTICLE_STATUS.OFFLINE"
							:disabled="articleDetail?.status === ARTICLE_STATUS.DRAFT"
						/>
					</el-select>
				</el-form-item>

				<el-form-item
					label="是否置顶"
					prop="isTop"
				>
					<el-switch v-model="upsertRequest.isTop" />
				</el-form-item>

				<el-form-item
					label="是否允许评论"
					prop="allowComment"
				>
					<el-switch v-model="upsertRequest.allowComment" />
				</el-form-item>
			</el-form>

			<!-- md 编辑器 -->
			<MdEditor
				v-model="upsertRequest.contentMd"
				class="rounded-2xl"
				:theme="resolvedTheme"
			/>
		</el-card>

		<!-- 点击按钮发送请求 -->
		<div class="admin-article-edit-footer my-12 flex w-full justify-center">
			<!-- 🔺防坑，如果要写成行内，别忘了加了括号就是立即运行，所以这里的 @click 要写成箭头函数。 -->
			<!-- e.g. @click="() => mode === 'create' ? handleCreateArticle() : handleUpdateArticle(articleId ?? 0)" -->
			<el-button
				type="primary"
				size="large"
				class="w-full"
				:disabled="submitting"
				:loading="submitting"
				@click="clickUpsertArticle"
			>
				{{ mode === 'create' ? '创建文章' : '更新文章' }}
			</el-button>
		</div>

		<!-- 选择标签 -->
		<el-dialog
			v-model="tagWindowVisible"
			title="选择标签"
			@close="tagDialogCancel"
		>
			<el-check-tag
				v-for="tag in tmpCheckTagList"
				:key="tag.id"
				:checked="tag.checked"
				@change="tag.checked = !tag.checked"
				class="admin-article-check-tag m-2"
				type="primary"
			>
				{{ tag.name }}
			</el-check-tag>
			<div class="mt-4 flex justify-center">
				<el-button
					type="primary"
					@click="tagDialogConfirm"
					>确认</el-button
				>
				<el-button @click="tagDialogCancel">取消</el-button>
			</div>
		</el-dialog>
	</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { MdEditor } from 'md-editor-v3';
import { useRoute } from 'vue-router';
import { useAdminCategoryList } from '../../category/composables/useAdminCategoryList';
import { useAdminTagList } from '../../tag/composables/useAdminTagList';
import { useAdminArticleDetail } from '../composables/useAdminArticleDetail';
import { useAdminArticleForm } from '../composables/useAdminArticleForm';
import { useTheme } from '@/composables/useTheme';
import { ARTICLE_STATUS, type CheckTagItem } from '../types/adminArticle';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance } from 'element-plus';

const route = useRoute();
const router = useRouter();

const { categoryList, getCategoryList } = useAdminCategoryList();

const { tagList, getTagList } = useAdminTagList();

const { articleDetail, handleGetArticleDetails } = useAdminArticleDetail();

const { submitting, upsertRequest, rules, handleCreateArticle, handleUpdateArticle } =
	useAdminArticleForm();

const { resolvedTheme } = useTheme();

// 表单引用
let theFormRef = ref<FormInstance>();

// 控制标签选择窗口是否可见
let tagWindowVisible = ref(false);

// 标签选择列表
let checkTagList = ref<CheckTagItem[]>([]);
let tmpCheckTagList = ref<CheckTagItem[]>([]);

defineOptions({
	name: 'AdminArticleEditPage',
});

// 当前文章id（创建文章时为 null）
let articleId: number | null = null;

// 当前模式
let mode = ref<'create' | 'edit'>('create');

onMounted(async () => {
	await getCategoryList();
	await getTagList();

	articleId = Number(route.params.articleId);
	if (!isNaN(articleId)) {
		// 更新文章
		mode.value = 'edit';

		// 获取文章详情并填充 articleDetail
		const success = await handleGetArticleDetails(articleId);
		if (!success) {
			// 获取失败
			router.replace('/admin/articles');
			return;
		}

		upsertRequest.title = articleDetail?.title || '';
		upsertRequest.summary = articleDetail?.summary || '';
		upsertRequest.contentMd = articleDetail?.contentMd || '';
		upsertRequest.categoryId = articleDetail?.categoryId || null;
		upsertRequest.tagIds = articleDetail?.tagIds || [];
		upsertRequest.coverUrl = articleDetail?.coverUrl || '';
		upsertRequest.isTop = articleDetail?.isTop || false;
		upsertRequest.status = articleDetail?.status || ARTICLE_STATUS.DRAFT;
		upsertRequest.allowComment = articleDetail?.allowComment || false;
	} else {
		// 创建文章
		mode.value = 'create';
	}

	tagList.value.forEach((tag) => {
		checkTagList.value.push({
			...tag,
			checked: upsertRequest.tagIds?.includes(tag.id) || false,
		});
	});
});

function showTagDialog() {
	tmpCheckTagList.value = JSON.parse(JSON.stringify(checkTagList.value));
	tagWindowVisible.value = true;
}

function tagDialogConfirm() {
	checkTagList.value = JSON.parse(JSON.stringify(tmpCheckTagList.value));
	upsertRequest.tagIds = checkTagList.value.filter((tag) => tag.checked).map((tag) => tag.id);
	tagWindowVisible.value = false;
}

function tagDialogCancel() {
	tagWindowVisible.value = false;
}

async function clickUpsertArticle() {
	try {
		if (!upsertRequest.contentMd?.trim()) {
			throw new Error('文章正文不能为空');
		}
		await theFormRef.value?.validate();
	} catch (error) {
		ElMessage.error(error instanceof Error ? error.message : '表单验证失败');
		return;
	}

	let success: boolean;
	if (mode.value === 'create') {
		success = await handleCreateArticle();
	} else {
		success = await handleUpdateArticle(articleId ?? 0);
	}

	if (success) {
		router.push('/admin/articles');
	}
}
</script>

<style lang="scss" scoped>
.admin-article-tag {
	font-size: small;
	font-weight: 450;
}
.admin-article-check-tag {
	transition: none;
	font-weight: 450;
}
</style>
