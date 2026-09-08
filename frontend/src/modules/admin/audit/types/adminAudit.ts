import type { ApiResult, PageResult } from '@/types/api';

export const AUDIT_RESOURCE_TYPE = {
	USER: 'USER',
	ARTICLE: 'ARTICLE',
	CATEGORY: 'CATEGORY',
	TAG: 'TAG',
	COMMENT: 'COMMENT',
	MESSAGE: 'MESSAGE',
	FILE: 'FILE',
} as const;

export type AuditResourceType = (typeof AUDIT_RESOURCE_TYPE)[keyof typeof AUDIT_RESOURCE_TYPE];

export const AUDIT_ACTION = {
	CREATE: 'CREATE',
	UPDATE: 'UPDATE',
	DELETE: 'DELETE',
	CHANGE_STATUS: 'CHANGE_STATUS',
	CHANGE_ROLE: 'CHANGE_ROLE',
	MODERATE: 'MODERATE',
	REPLY: 'REPLY',
	BATCH_APPROVE: 'BATCH_APPROVE',
	UPLOAD: 'UPLOAD',
} as const;

export type AuditAction = (typeof AUDIT_ACTION)[keyof typeof AUDIT_ACTION];

export const AUDIT_RESULT = {
	SUCCESS: 'SUCCESS',
	FAILURE: 'FAILURE',
} as const;

export type AuditResult = (typeof AUDIT_RESULT)[keyof typeof AUDIT_RESULT];

export interface AdminAuditLogFilterForm {
	operatorId?: number;
	resourceType?: AuditResourceType | '';
	resourceId?: string;
	action?: AuditAction | '';
	result?: AuditResult | '';
	createdAtRange?: [string, string] | [];
}

export interface AdminAuditLogListQuery {
	pageNum?: number;
	pageSize?: number;
	operatorId?: number;
	resourceType?: AuditResourceType;
	resourceId?: string;
	action?: AuditAction;
	result?: AuditResult;
	createdAtFrom?: string;
	createdAtTo?: string;
}

export interface AdminAuditLogItem {
	id: number;
	operatorId: number;
	operatorUsername: string;
	resourceType: AuditResourceType;
	resourceId: string | null;
	action: AuditAction;
	actionDetail: string | null;
	result: AuditResult;
	failureCode: number | null;
	failureMessage: string | null;
	requestMethod: string;
	requestPath: string;
	createdAt: string;
}

export type AdminAuditLogPageData = PageResult<AdminAuditLogItem>;
export type AdminAuditLogPageResponse = ApiResult<AdminAuditLogPageData>;
