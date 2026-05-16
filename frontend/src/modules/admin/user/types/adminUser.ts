import type { ApiResult, PageResult } from '@/types/api';

import type { UserRole, UserStatus } from '@/modules/user/types/user';

export interface AdminUserListQuery {
	pageNum?: number;
	pageSize?: number;
	keyword?: string;
	role?: UserRole;
	status?: UserStatus;
}

export interface AdminUserListItem {
	id: number;
	username: string;
	nickname: string;
	email: string;
	role: UserRole;
	status: UserStatus;
	lastLoginAt: string | null;
	createdAt: string;
}

export type AdminUserPageData = PageResult<AdminUserListItem>;

export type AdminUserPageResponse = ApiResult<AdminUserPageData>;

export interface UpdateUserStatusRequest {
	status: UserStatus;
}

export interface UpdateUserStatusData {
	id: number;
	status: UserStatus;
}

export type UpdateUserStatusResponse = ApiResult<UpdateUserStatusData>;

export interface UpdateUserRoleRequest {
	role: UserRole;
}

export interface UpdateUserRoleData {
	id: number;
	role: UserRole;
}

export type UpdateUserRoleResponse = ApiResult<UpdateUserRoleData>;
