import request from '@/utils/request';
import type {
	AdminAboutPageData,
	AdminAboutPageResponse,
	UpdateAboutPageRequest,
	UpdateAboutPageResponse,
} from '../types/adminAbout';

const ADMIN_ABOUT_API = {
	getAboutPage: 'admin/about',
	updateAboutPage: 'admin/about',
} as const;

// 获取后台关于页编辑数据
export const getAboutPage = (): Promise<AdminAboutPageData> => {
	return request.get<AdminAboutPageResponse, AdminAboutPageData>(ADMIN_ABOUT_API.getAboutPage);
};

// 保存后台关于页
export const updateAboutPage = (data: UpdateAboutPageRequest): Promise<AdminAboutPageData> => {
	return request.put<UpdateAboutPageResponse, AdminAboutPageData, UpdateAboutPageRequest>(
		ADMIN_ABOUT_API.updateAboutPage,
		data,
	);
};
