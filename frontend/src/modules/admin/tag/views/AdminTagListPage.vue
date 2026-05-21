<template>
	<div class="admin-tag">
		<el-card>
			<template #header>
				<div class="header flex items-center justify-between">
					<span class="text-xl font-bold">标签列表</span>
					<el-button
						type="primary"
						class="m-4"
						@click="openCreateDrawer"
					>
						新建标签
					</el-button>
				</div>
			</template>
			<template #default>
				<!-- 查询条件 -->
				<el-form
					label-width="auto"
					label-position="right"
					:model="queryParams"
				>
					<div class="mb-6 flex">
						<el-input
							v-model.trim="queryParams.keyword"
							placeholder="请输入标签名称模糊搜索"
						/>
						<el-button
							type="primary"
							class="ml-2"
							aria-label="搜索"
							@click="getTagList"
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
							@click="
								resetQueryParams();
								getTagList();
							"
						>
							<template #icon>
								<i-lets-icons-refresh />
							</template>
							重置
						</el-button>
					</div>

					<el-form-item
						prop="status"
						label="标签状态"
					>
						<el-radio-group
							v-model="queryParams.status"
							@change="getTagList"
						>
							<el-radio :value="''">全部</el-radio>
							<el-radio :value="TAG_STATUS.ENABLED">启用</el-radio>
							<el-radio :value="TAG_STATUS.DISABLED">禁用</el-radio>
						</el-radio-group>
					</el-form-item>
				</el-form>

				<!-- 标签列表 -->
				<el-table
					:data="tagList"
					row-key="id"
					border
				>
					<el-table-column
						prop="id"
						align="center"
						width="100"
						label="ID"
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
						label="状态"
						align="center"
						width="80"
						prop="status"
					>
						<template #default="{ row }">
							<div class="flex items-center justify-center">
								<i-lets-icons-check-fill
									class="text-xl text-green-600"
									v-if="row.status == TAG_STATUS.ENABLED"
								/>
								<i-lets-icons-cancel
									class="text-xl text-red-600"
									v-else
								/>
							</div>
						</template>
					</el-table-column>

					<el-table-column
						label="文章数量"
						align="center"
						width="80"
						prop="articleCount"
					>
					</el-table-column>

					<el-table-column
						label="创建时间"
						align="center"
						width="200"
						prop="createdAt"
					>
						<template #default="{ row }">
							{{ formatDateTime(row.createdAt) }}
						</template>
					</el-table-column>

					<el-table-column
						label="标签操作"
						align="center"
						width="400"
						fixed="right"
					>
						<template #default="{ row }: { row: AdminTagItem }">
							<el-button
								type="warning"
								title="编辑标签信息"
								@click="openUpdateDrawer(row)"
							>
								<template #icon>
									<i-ep-edit />
								</template>
								编辑
							</el-button>

							<el-popconfirm
								:title="`确认修改 ${row.name} 的状态为 ${row.status === TAG_STATUS.ENABLED ? '禁用' : '启用'} ？`"
								@confirm="toggleTagStatus(row)"
							>
								<template #reference>
									<el-button
										:type="
											row.status === TAG_STATUS.ENABLED ? 'danger' : 'success'
										"
										:title="
											row.status === TAG_STATUS.ENABLED
												? `禁用标签：${row.name}`
												: `启用标签：${row.name}`
										"
									>
										<template #icon>
											<i-ep-switch />
										</template>
										{{ row.status === TAG_STATUS.ENABLED ? '禁用' : '启用' }}
									</el-button>
								</template>
							</el-popconfirm>

							<el-popconfirm
								:title="`确认删除标签 ${row.name} 吗？`"
								@confirm="handleDeleteTag(row)"
							>
								<template #reference>
									<el-button
										type="danger"
										title="删除标签"
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
			</template>
		</el-card>

		<!-- 新建 / 编辑标签抽屉 -->
		<el-drawer
			v-model="showDrawer"
			resizable
			direction="rtl"
		>
			<template #header>
				<p class="font-bold">{{ drawerMode === 'create' ? '新建' : '编辑' }}标签</p>
			</template>

			<template #default>
				<el-form
					:model="tagForm"
					label-width="auto"
					label-position="right"
					:ref="setTagFormRef"
					:rules="rules"
				>
					<el-form-item
						prop="name"
						label="标签名称"
					>
						<el-input
							v-model.trim="tagForm.name"
							placeholder="请输入标签名称"
						></el-input>
					</el-form-item>

					<el-form-item
						prop="description"
						label="标签描述"
					>
						<el-input
							type="textarea"
							v-model.trim="tagForm.description"
							placeholder="请输入标签描述"
						></el-input>
					</el-form-item>

					<el-form-item
						prop="status"
						label="状态"
					>
						<el-select
							v-model="tagForm.status"
							placeholder="请选择标签状态"
						>
							<el-option
								:value="TAG_STATUS.ENABLED"
								label="启用"
							/>
							<el-option
								:value="TAG_STATUS.DISABLED"
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
					@click="submitTag"
				>
					确定
				</el-button>
			</template>
		</el-drawer>
	</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
defineOptions({
	name: 'AdminTagListPage',
});

import { useAdminTagList } from '../composables/useAdminTagList';
import { TAG_STATUS, type AdminTagItem } from '../types/adminTag';
import { formatDateTime } from '@/utils/datetime';
const { tagList, queryParams, getTagList, resetQueryParams, handleDeleteTag, toggleTagStatus } =
	useAdminTagList();

import { useAdminTagForm } from '../composables/useAdminTagForm';
const {
	showDrawer,
	drawerMode,
	tagForm,
	rules,
	openCreateDrawer,
	openUpdateDrawer,
	setTagFormRef,
	handleUpsertTag,
} = useAdminTagForm();

onMounted(() => {
	getTagList();
});

// 点击抽屉中的“确定”按钮时，调用 handleUpsertTag 处理函数发送请求，成功后重新刷新标签列表
async function submitTag() {
	const success = await handleUpsertTag();
	if (success) {
		getTagList();
	}
}
</script>

<style></style>
