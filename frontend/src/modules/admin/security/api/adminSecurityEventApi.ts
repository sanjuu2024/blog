import request from '@/utils/request';
import type {
	SecurityEventListQuery,
	SecurityEventPageData,
	SecurityEventPageResponse,
} from '../types/adminSecurityEvent';

const ADMIN_SECURITY_EVENT_API = {
	listSecurityEvents: 'admin/security-events',
} as const;

// 获取后台安全事件分页列表
export const listSecurityEvents = (
	params?: SecurityEventListQuery,
): Promise<SecurityEventPageData> => {
	return request.get<SecurityEventPageResponse, SecurityEventPageData, SecurityEventListQuery>(
		ADMIN_SECURITY_EVENT_API.listSecurityEvents,
		{ params },
	);
};
