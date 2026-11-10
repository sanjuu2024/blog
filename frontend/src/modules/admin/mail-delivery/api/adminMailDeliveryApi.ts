import request from '@/utils/request';
import type {
	AdminMailDeliveryItem,
	AdminMailDeliveryListQuery,
	AdminMailDeliveryNoDataResponse,
	AdminMailDeliveryPageData,
	AdminMailDeliveryPageResponse,
	AdminMailDeliveryResponse,
} from '../types/adminMailDelivery';

const API = {
	list: 'admin/mail-deliveries',
	item: (id: number) => `admin/mail-deliveries/${id}`,
	retry: (id: number) => `admin/mail-deliveries/${id}/retry`,
} as const;

// 获取后台邮件投递分页列表接口
export const listMailDeliveries = (params: AdminMailDeliveryListQuery) =>
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.get<后端原始响应, 拦截器最终返回值, 查询参数类型>(url, { params: params })
	request.get<
		AdminMailDeliveryPageResponse,
		AdminMailDeliveryPageData,
		AdminMailDeliveryListQuery
	>(API.list, { params });

// 获取邮件投递详情接口
export const getMailDelivery = (id: number) =>
	// 🔺request.get<后端原始响应, 拦截器最终返回值, 请求体类型>(url)
	request.get<AdminMailDeliveryResponse, AdminMailDeliveryItem, null>(API.item(id));

// 手动重试失败邮件接口
export const retryMailDelivery = (id: number): Promise<null> =>
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url)
	request.post<AdminMailDeliveryNoDataResponse, null, null>(API.retry(id));
