import request from '@/utils/request';
import type {
	UpdateUserStatusData,
	UpdateUserStatusResponse,
	AdminUserListQuery,
	AdminUserPageResponse,
	UpdateUserRoleData,
	UpdateUserRoleRequest,
	UpdateUserRoleResponse,
	UpdateUserStatusRequest,
	AdminUserPageData,
} from '../types/adminUser';

const ADMIN_USER_API = {
	listUsers: 'admin/users',
	updateStatus: (userId: number) => `admin/users/${userId}/status`,
	updateRole: (userId: number) => `admin/users/${userId}/role`,
} as const;

// 获取用户分页列表接口
export const listUsers = (params?: AdminUserListQuery): Promise<AdminUserPageData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<AdminUserPageResponse, AdminUserPageData, AdminUserListQuery>(
		ADMIN_USER_API.listUsers,
		{
			params,
			meta: {
				showError: false, // 列表接口请求失败时不显示默认错误提示，由调用者函数自己处理
			},
		},
	);
};

// 修改用户状态接口
export const updateStatus = (
	userId: number,
	data: UpdateUserStatusRequest,
): Promise<UpdateUserStatusData> => {
	return request.patch<UpdateUserStatusResponse, UpdateUserStatusData, UpdateUserStatusRequest>(
		ADMIN_USER_API.updateStatus(userId),
		data,
	);
};

// 修改用户角色接口
export const updateRole = (
	userId: number,
	data: UpdateUserRoleRequest,
): Promise<UpdateUserRoleData> => {
	return request.patch<UpdateUserRoleResponse, UpdateUserRoleData, UpdateUserRoleRequest>(
		ADMIN_USER_API.updateRole(userId),
		data,
	);
};
