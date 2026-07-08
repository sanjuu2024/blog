<template>
	<div class="admin-category">
		<el-card class="admin-category-list-card">
			<template #header>
				<div class="header flex items-center justify-between">
					<span class="text-xl font-bold">分类列表</span>
					<el-button
						type="primary"
						class="m-4"
						@click="openCreateDrawer()"
					>
						新建分类
					</el-button>
				</div>
			</template>

			<template #default>
				<!-- @submit.prevent：阻止表单的原生回车提交 -->
				<el-form
					label-width="auto"
					label-position="right"
					:model="queryParams"
					@submit.prevent
				>
					<!-- <el-form-item
						prop="keyword"
						label="关键字搜索"
					> -->
					<div class="mb-6 flex">
						<!-- 搜索框内按下回车键也触发搜索 -->
						<el-input
							v-model.trim="queryParams.keyword"
							placeholder="请输入分类名称模糊搜索"
							@keyup.enter="getCategoryList"
						/>
						<el-button
							type="primary"
							class="ml-2"
							aria-label="搜索"
							@click="getCategoryList"
						>
							<template #icon>
								<i-lets-icons-search-alt />
							</template>
							搜索
						</el-button>

						<el-button
							type="warning"
							class="ml-2"
							aria-label="重置"
							@click="resetQueryParams()"
						>
							<template #icon>
								<i-lets-icons-refresh />
							</template>
							重置
						</el-button>
					</div>
					<!-- </el-form-item> -->

					<div class="flex">
						<el-form-item
							prop="level"
							class="mr-4 w-80"
							label="分类级别"
						>
							<el-select
								v-model="queryParams.level"
								placeholder="请选择分类级别"
								@change="getCategoryList"
							>
								<el-option
									:value="'ALL'"
									label="全部"
								/>
								<el-option
									:value="1"
									label="一级分类"
								/>
								<el-option
									:value="2"
									label="二级分类"
								/>
							</el-select>
						</el-form-item>

						<el-form-item
							prop="parentId"
							class="w-80"
							label="父级分类"
							v-if="queryParams.level === 2"
						>
							<el-select
								v-model="queryParams.parentId"
								placeholder="请选择父级分类"
								@change="getCategoryList"
							>
								<el-option
									v-for="category in parentCategoryOptions"
									:key="category.id"
									:value="category.id"
									:label="category.name"
								/>
							</el-select>
						</el-form-item>
					</div>

					<el-form-item
						prop="status"
						label="分类状态"
					>
						<el-radio-group
							v-model="queryParams.status"
							@change="getCategoryList"
						>
							<el-radio :value="''">全部</el-radio>
							<el-radio :value="CATEGORY_STATUS.ENABLED">启用</el-radio>
							<el-radio :value="CATEGORY_STATUS.DISABLED">禁用</el-radio>
						</el-radio-group>
					</el-form-item>
				</el-form>

				<!-- 分类列表 -->
				<div class="admin-category-table-wrap">
					<el-table
						ref="categoryTableRef"
						:data="categoryList"
						row-key="id"
						:lazy="false"
						:tree-props="{ children: 'children' }"
						default-expand-all
						class="admin-category-table"
						height="100%"
					>
						<el-table-column
							prop="level"
							align="center"
							width="100"
							label="级别"
						/>
						<el-table-column
							prop="name"
							align="center"
							width="100"
							label="名称"
						/>
						<el-table-column
							prop="description"
							align="center"
							label="描述"
						/>
						<el-table-column
							prop="sortNo"
							align="center"
							label="排序值"
							width="80"
						/>

						<el-table-column
							label="状态"
							align="center"
							width="80"
							prop="status"
						>
							<template #default="{ row }">
								<div class="flex items-center justify-center">
									<i-lets-icons-check-fill
										class="text-xl text-green-600"
										v-if="row.status == CATEGORY_STATUS.ENABLED"
									/>
									<i-lets-icons-cancel
										class="text-xl text-red-600"
										v-else
									/>
								</div>
							</template>
						</el-table-column>

						<el-table-column
							prop="createdAt"
							align="center"
							width="180"
							label="创建时间"
						>
							<template #default="{ row }">
								{{ formatDateTime(row.createdAt) }}
							</template>
						</el-table-column>

						<el-table-column
							label="分类操作"
							align="center"
							width="400"
							fixed="right"
						>
							<template #default="{ row }: { row: AdminCategoryItem }">
								<el-button
									type="warning"
									title="编辑分类信息"
									@click="openUpdateDrawer(row)"
								>
									<template #icon>
										<i-ep-edit />
									</template>
									编辑
								</el-button>

								<el-popconfirm
									:title="`确认修改 ${row.name} 的状态为 ${row.status === CATEGORY_STATUS.ENABLED ? '禁用' : '启用'} ？`"
									@confirm="toggleCategoryStatus(row)"
								>
									<template #reference>
										<el-button
											:type="
												row.status === CATEGORY_STATUS.ENABLED
													? 'danger'
													: 'success'
											"
											:title="
												row.status === CATEGORY_STATUS.ENABLED
													? `禁用分类：${row.name}`
													: `启用分类：${row.name}`
											"
										>
											<template #icon>
												<i-ep-switch />
											</template>
											{{
												row.status === CATEGORY_STATUS.ENABLED
													? '禁用'
													: '启用'
											}}
										</el-button>
									</template>
								</el-popconfirm>

								<el-popconfirm
									:title="`确认删除分类 ${row.name} 吗？`"
									@confirm="clickDeleteCategory(row.id)"
								>
									<template #reference>
										<el-button
											type="danger"
											title="删除分类"
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
				</div>
			</template>
		</el-card>

		<!-- 新建 / 编辑 分类时显示的抽屉 -->
		<el-drawer
			v-model="showDrawer"
			resizable
			direction="rtl"
		>
			<template #header>
				<p class="font-bold">{{ drawerMode === 'create' ? '新建' : '编辑' }}分类</p>
			</template>

			<template #default>
				<el-form
					label-width="auto"
					label-position="left"
					:model="categoryForm"
					:rules="rules"
					:ref="setCategoryFormRef"
				>
					<el-form-item
						prop="level"
						label="级别"
					>
						<el-select
							v-model="categoryForm.level"
							placeholder="请选择分类级别"
						>
							<el-option
								:value="1"
								label="一级分类"
							/>
							<el-option
								:value="2"
								label="二级分类"
							/>
						</el-select>
					</el-form-item>

					<el-form-item
						prop="parentId"
						label="父级分类"
						v-if="categoryForm.level == 2"
					>
						<el-select
							v-model="categoryForm.parentId"
							placeholder="请选择父级分类"
						>
							<el-option
								v-for="category in parentCategoryOptions"
								:key="category.id"
								:value="category.id"
								:label="category.name"
							/>
						</el-select>
					</el-form-item>

					<el-form-item
						prop="name"
						label="分类名称"
					>
						<el-input
							v-model.trim="categoryForm.name"
							placeholder="请输入分类名称"
						></el-input>
					</el-form-item>

					<el-form-item
						prop="description"
						label="分类描述"
					>
						<el-input
							type="textarea"
							v-model.trim="categoryForm.description"
							placeholder="请输入分类描述"
						></el-input>
					</el-form-item>

					<el-form-item
						prop="sortNo"
						label="排序值"
					>
						<el-input
							v-model.number="categoryForm.sortNo"
							:min="0"
							:max="999"
							placeholder="请输入排序值，数值越小越靠前"
						/>
					</el-form-item>

					<el-form-item
						prop="status"
						label="状态"
					>
						<el-select
							v-model="categoryForm.status"
							placeholder="请选择分类状态"
						>
							<el-option
								:value="CATEGORY_STATUS.ENABLED"
								label="启用"
							/>
							<el-option
								:value="CATEGORY_STATUS.DISABLED"
								label="禁用"
							/>
						</el-select>
					</el-form-item>
				</el-form>
			</template>
			<template #footer>
				<el-button @click="showDrawer = false">取消</el-button>
				<el-button
					type="primary"
					@click="submitCategory"
				>
					确定
				</el-button>
			</template>
		</el-drawer>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import type { TableInstance } from 'element-plus';
import { formatDateTime } from '@/utils/datetime';
import type { AdminCategoryItem } from '../types/adminCategory';

import { useAdminCategoryForm } from '../composables/useAdminCategoryForm';
const {
	showDrawer,
	drawerMode,
	categoryForm,
	rules,
	setCategoryFormRef,
	handleUpsertCategory,
	openCreateDrawer,
	openUpdateDrawer,
} = useAdminCategoryForm();

import { useAdminCategoryList } from '../composables/useAdminCategoryList';
const {
	categoryList,
	queryParams,
	getCategoryList,
	toggleCategoryStatus,
	handleDeleteCategory,
	resetQueryParams,
} = useAdminCategoryList();

import { useAdminCategoryOptions } from '../composables/useAdminCategoryOptions';
import { CATEGORY_STATUS } from '@/modules/category/constants/category';
const { parentCategoryOptions, getParentCategoryOptions } = useAdminCategoryOptions();

const categoryTableRef = ref<TableInstance>();

defineOptions({
	name: 'AdminCategoryListPage',
});

onMounted(() => {
	getCategoryList();
	getParentCategoryOptions();
});

watch(categoryList, async () => {
	await nextTick();
	categoryTableRef.value?.setScrollTop(0);
});

// 点击抽屉中的“确定”按钮时，调用 handleUpsertCategory（校验并发送请求），成功后刷新分类列表和父级分类选项
async function submitCategory() {
	const success = await handleUpsertCategory(categoryForm);

	if (!success) return;

	await getCategoryList();
	await getParentCategoryOptions();
}

// 点击删除分类时，调用 handleDeleteCategory 处理函数发送请求，成功后重新刷新分类列表、一级分类下拉列表
async function clickDeleteCategory(categoryId: number) {
	const success = await handleDeleteCategory(categoryId);
	if (success) {
		await getParentCategoryOptions();
	}
}
</script>

<style scoped lang="scss">
.admin-category {
	height: 100%;
	min-height: 0;
}

.admin-category-list-card {
	display: flex;
	height: 100%;
	flex-direction: column;
	box-shadow: none;
	border: none;
}

.admin-category-list-card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-category-table-wrap {
	min-height: 0;
	flex: 1;
}

// 自定义表格样式，覆盖 Element Plus 默认的行 hover
.admin-category-table {
	--el-table-row-hover-bg-color: var(--app-table-row-hover-bg-color);
	height: 100%;
	width: 100%;
}
</style>
