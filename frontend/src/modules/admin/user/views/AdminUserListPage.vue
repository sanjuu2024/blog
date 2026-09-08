<template>
	<el-card class="admin-user-list-card">
		<template #header>
			<div class="header flex items-center justify-between">
				<span class="text-xl font-bold">用户列表</span>
			</div>
		</template>

		<!-- 筛选条件 -->
		<div class="mb-4 flex gap-2">
			<el-input
				v-model.trim="listQuery.username"
				maxlength="20"
				clearable
				placeholder="请输入用户名"
				@keyup.enter="getUserList(1)"
			/>
			<el-input
				v-model.trim="listQuery.email"
				maxlength="255"
				clearable
				placeholder="请输入邮箱"
				@keyup.enter="getUserList(1)"
			/>
			<el-button
				type="primary"
				aria-label="搜索"
				@click="getUserList(1)"
			>
				<i-lets-icons-search-alt />
			</el-button>
		</div>

		<el-form
			label-width="auto"
			:model="listQuery"
			class="pl-0.5"
		>
			<el-form-item label="用户角色">
				<el-radio-group
					v-model="listQuery.role"
					@change="getUserList(1)"
				>
					<el-radio
						label="全部"
						value=""
						>全部</el-radio
					>
					<el-radio
						label="管理员"
						:value="USER_ROLE.ADMIN"
						>管理员</el-radio
					>
					<el-radio
						label="普通用户"
						:value="USER_ROLE.USER"
						>普通用户</el-radio
					>
				</el-radio-group>
			</el-form-item>
			<el-form-item label="用户状态">
				<el-radio-group
					v-model="listQuery.status"
					@change="getUserList(1)"
				>
					<el-radio
						label="全部"
						value=""
						>全部</el-radio
					>
					<el-radio
						label="正常"
						:value="USER_STATUS.ACTIVE"
						>正常</el-radio
					>
					<el-radio
						label="禁用"
						:value="USER_STATUS.DISABLED"
						>禁用</el-radio
					>
				</el-radio-group>
			</el-form-item>
		</el-form>

		<!-- <hr class="text-gray-400 my-4"/> -->

		<!-- 表格部分 -->
		<div class="admin-user-table-wrap">
			<el-table
				ref="userTableRef"
				:data="userList"
				class="admin-user-table"
				height="100%"
			>
				<el-table-column
					label="序号"
					align="center"
					width="80px;"
					type="index"
				></el-table-column>
				<el-table-column
					label="用户 ID"
					align="center"
					prop="id"
				></el-table-column>
				<el-table-column
					label="用户名"
					align="center"
					prop="username"
					width="100"
				></el-table-column>
				<el-table-column
					label="昵称"
					align="center"
					prop="nickname"
					width="100"
				></el-table-column>
				<el-table-column
					label="邮箱"
					align="center"
					prop="email"
					width="100"
				></el-table-column>
				<el-table-column
					label="角色"
					align="center"
					prop="role"
				></el-table-column>
				<el-table-column
					label="状态"
					align="center"
					prop="status"
				>
					<template #default="{ row }">
						<div class="flex items-center justify-center">
							<i-lets-icons-check-fill
								class="text-xl text-(--app-icon-green-color)"
								v-if="row.status == USER_STATUS.ACTIVE"
							/>
							<i-lets-icons-cancel
								class="text-xl text-(--app-icon-red-color)"
								v-else
							/>
						</div>
					</template>
				</el-table-column>
				<el-table-column
					label="上次登录时间"
					align="center"
					prop="lastLoginAt"
					width="120"
				>
					<template #default="{ row }">
						{{ formatDateTime(row.lastLoginAt) }}
					</template>
				</el-table-column>
				<el-table-column
					label="注册时间"
					align="center"
					prop="createdAt"
					width="100"
				>
					<template #default="{ row }">
						{{ formatDateTime(row.createdAt) }}
					</template>
				</el-table-column>
				<el-table-column
					label="用户操作"
					align="center"
					width="300"
					fixed="right"
				>
					<template #default="{ row }">
						<el-popconfirm
							:title="`确认修改 ${row.username} 的状态为 ${row.status === USER_STATUS.ACTIVE ? '禁用' : '正常'} ？`"
							@confirm="toggleUserStatus(row)"
						>
							<template #reference>
								<el-button
									type="warning"
									title="修改用户状态"
									:disabled="userStore.userInfo?.id === row.id"
								>
									<template #icon>
										<i-ep-edit />
									</template>
									修改状态
								</el-button>
							</template>
						</el-popconfirm>
						<el-popconfirm
							:title="`确认修改 ${row.username} 为 ${row.role === USER_ROLE.ADMIN ? '普通用户' : '管理员'} ？`"
							@confirm="toggleUserRole(row)"
						>
							<template #reference>
								<el-button
									type="warning"
									title="修改用户角色"
									:disabled="userStore.userInfo?.id === row.id"
								>
									<template #icon>
										<i-solar-user-id-linear />
									</template>
									修改角色
								</el-button>
							</template>
						</el-popconfirm>
					</template>
				</el-table-column>
			</el-table>
		</div>

		<!-- 分页部分 -->
		<!-- 注意 current-page 和 page-size 是 v-model 双向绑定，而不只是 v-bind -->
		<el-pagination
			v-model:current-page="pageNum"
			v-model:page-size="pageSize"
			:background="true"
			layout="prev, pager, next, jumper, ->, sizes, total"
			:total="total"
			:pager-count="pagerCount"
			:page-sizes="[3, 5, 7, 9]"
			@current-change="getUserList"
			@size-change="getUserList"
			class="admin-user-pagination"
		/>
	</el-card>
</template>

<script setup lang="ts">
import { nextTick, ref, reactive, onMounted, watch } from 'vue';
import type { TableInstance } from 'element-plus';
import type {
	AdminUserListItem,
	AdminUserListQuery,
	AdminUserPageData,
	UpdateUserRoleData,
	UpdateUserStatusData,
} from '../types/adminUser';
import { listUsers, updateRole, updateStatus } from '../api/adminUserApi';
import { ElMessage } from 'element-plus';
import { formatDateTime } from '@/utils/datetime';
import { USER_ROLE, USER_STATUS, type UserRole, type UserStatus } from '@/modules/user/types/user';
import { useUserStore } from '@/stores/userStore';

type UserRoleFilter = UserRole | '';
type UserStatusFilter = UserStatus | '';

defineOptions({
	name: 'AdminUserListPage',
});

const userStore = useUserStore();

// 用户列表数据
let userList = ref<AdminUserListItem[]>([]);
const userTableRef = ref<TableInstance>();

// 分页参数
let pageNum = ref<number>(1); // 当前页
let pageSize = ref<number>(5); // 每页条数
let total = ref<number>(0); // 总条数
let pagerCount = ref<number>(5); // 当前分页器显示多少个页码按钮

// 条件查询参数
const listQuery = reactive({
	username: '',
	email: '',
	role: '' as UserRoleFilter,
	status: '' as UserStatusFilter,
});

// 获取分页列表
async function getUserList(page: number = pageNum.value) {
	pageNum.value = page;
	try {
		// role 为空字符串时为 false，则会被设置为 undefined
		const data: AdminUserPageData = await listUsers({
			pageNum: page,
			pageSize: pageSize.value,
			username: listQuery.username,
			email: listQuery.email,
			role: listQuery.role || undefined,
			status: listQuery.status || undefined,
		} as AdminUserListQuery);

		userList.value = data.records;
		pageSize.value = data.pageSize;
		total.value = data.total;
	} catch {
		ElMessage.error('获取用户列表失败，请稍后重试。');
	}
}

// 修改用户状态
async function toggleUserStatus(user: AdminUserListItem) {
	try {
		const data: UpdateUserStatusData = await updateStatus(user.id, {
			status: user.status === USER_STATUS.ACTIVE ? USER_STATUS.DISABLED : USER_STATUS.ACTIVE,
		} as UpdateUserStatusData);
		ElMessage.success('用户状态修改成功');
		user.status = data.status; // 直接修改当前行数据的 status 字段，无需重新拉取列表
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	}
}

// 修改用户角色
async function toggleUserRole(user: AdminUserListItem) {
	try {
		const data: UpdateUserRoleData = await updateRole(user.id, {
			role: user.role === USER_ROLE.ADMIN ? USER_ROLE.USER : USER_ROLE.ADMIN,
		} as UpdateUserRoleData);
		ElMessage.success('用户角色修改成功');
		user.role = data.role; // 直接修改当前行数据的 role 字段，无需重新拉取列表
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	}
}

onMounted(async () => {
	await getUserList();
});

watch(userList, async () => {
	await nextTick();
	userTableRef.value?.setScrollTop(0);
});
</script>

<style scoped lang="scss">
.admin-user-list-card {
	display: flex;
	height: 100%;
	flex-direction: column;
	box-shadow: none;
	border: none;
	background-color: var(--app-bg);
}

.admin-user-list-card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-user-table-wrap {
	min-height: 0;
	flex: 1;
}

// 自定义表格样式，覆盖 Element Plus 默认的行 hover
.admin-user-table {
	--el-table-row-hover-bg-color: var(--app-table-row-hover-bg-color);
	height: 100%;
}

.admin-user-pagination {
	flex: none;
	margin-top: 1.5rem;
}
</style>
