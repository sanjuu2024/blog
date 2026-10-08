import type { ApiResult, PageResult } from '@/types/api';

export const SECURITY_EVENT_TYPE = {
	REGISTER: 'REGISTER',
	LOGIN: 'LOGIN',
	LOGOUT: 'LOGOUT',
	TOKEN_REFRESH: 'TOKEN_REFRESH',
	EMAIL_VERIFICATION: 'EMAIL_VERIFICATION',
	PASSWORD_CHANGE: 'PASSWORD_CHANGE',
	EMAIL_CHANGE: 'EMAIL_CHANGE',
	USER_DELETE: 'USER_DELETE',
	USER_STATUS_CHANGE: 'USER_STATUS_CHANGE',
	USER_ROLE_CHANGE: 'USER_ROLE_CHANGE',
} as const;

export type SecurityEventType = (typeof SECURITY_EVENT_TYPE)[keyof typeof SECURITY_EVENT_TYPE];

export const SECURITY_EVENT_OUTCOME = {
	SUCCESS: 'SUCCESS',
	FAILURE: 'FAILURE',
} as const;

export type SecurityEventOutcome =
	(typeof SECURITY_EVENT_OUTCOME)[keyof typeof SECURITY_EVENT_OUTCOME];

export interface SecurityEventFilterForm {
	eventType: SecurityEventType | '';
	outcome: SecurityEventOutcome | '';
	userId?: number;
	createdAtRange: [string, string] | [];
}

export interface SecurityEventListQuery {
	pageNum?: number;
	pageSize?: number;
	eventType?: SecurityEventType;
	outcome?: SecurityEventOutcome;
	userId?: number;
	createdAtFrom?: string;
	createdAtTo?: string;
}

export interface SecurityEventItem {
	id: number;
	eventType: SecurityEventType;
	outcome: SecurityEventOutcome;
	userId: number | null;
	userUsername: string | null;
	userNickname: string | null;
	userDeleted: boolean | null;
	actorId: number | null;
	actorUsername: string | null;
	actorNickname: string | null;
	actorDeleted: boolean | null;
	account: string | null;
	ip: string | null;
	userAgent: string | null;
	requestMethod: string | null;
	requestPath: string | null;
	browser: string | null;
	operatingSystem: string | null;
	device: string | null;
	description: string;
	createdAt: string;
}

export type SecurityEventPageData = PageResult<SecurityEventItem>;
export type SecurityEventPageResponse = ApiResult<SecurityEventPageData>;
