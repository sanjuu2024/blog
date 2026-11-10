import request from '@/utils/request';
import type {
	AdminAuditLogListQuery,
	AdminAuditLogPageData,
	AdminAuditLogPageResponse,
} from '../types/adminAudit';

const ADMIN_AUDIT_API = {
	listAuditLogs: 'admin/audit-logs',
} as const;

// 获取后台操作审计日志分页列表
export const listAuditLogs = (params?: AdminAuditLogListQuery): Promise<AdminAuditLogPageData> => {
	return request.get<AdminAuditLogPageResponse, AdminAuditLogPageData, AdminAuditLogListQuery>(
		ADMIN_AUDIT_API.listAuditLogs,
		{ params },
	);
};
