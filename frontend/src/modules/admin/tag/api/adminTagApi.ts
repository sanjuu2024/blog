import request from '@/utils/request';
import type {
	AdminTagListQuery,
	AdminTagListData,
	AdminTagListResponse,
	TagUpsertRequest,
	CreatedTagData,
	CreatedTagResponse,
	UpdatedTagData,
	UpdatedTagResponse,
	DeleteTagResponse,
} from '../types/adminTag';

const ADMIN_TAG_API = {
	listTags: 'admin/tags',
	createTag: 'admin/tags',
	updateTag: (tagId: number) => `admin/tags/${tagId}`,
	deleteTag: (tagId: number) => `admin/tags/${tagId}`,
} as const;

// 获取标签列表接口
export const listTags = (params?: AdminTagListQuery): Promise<AdminTagListData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<AdminTagListResponse, AdminTagListData, AdminTagListQuery>(
		ADMIN_TAG_API.listTags,
		{ params },
	);
};

// 创建标签接口
export const createTag = (data: TagUpsertRequest): Promise<CreatedTagData> => {
	return request.post<CreatedTagResponse, CreatedTagData, TagUpsertRequest>(
		ADMIN_TAG_API.createTag,
		data,
	);
};

// 更新标签接口
export const updateTag = (tagId: number, data: TagUpsertRequest): Promise<UpdatedTagData> => {
	return request.put<UpdatedTagResponse, UpdatedTagData, TagUpsertRequest>(
		ADMIN_TAG_API.updateTag(tagId),
		data,
	);
};

// 删除标签接口
export const deleteTag = (tagId: number): Promise<null> => {
	return request.delete<DeleteTagResponse, null, null>(ADMIN_TAG_API.deleteTag(tagId));
};
