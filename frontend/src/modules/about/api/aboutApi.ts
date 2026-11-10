import request from '@/utils/request';
import type { PublicAboutPageData, PublicAboutPageResponse } from '../types/about';

const ABOUT_API = {
	getAboutPage: 'about',
} as const;

// 获取公开关于页内容
export const getAboutPage = (): Promise<PublicAboutPageData> => {
	return request.get<PublicAboutPageResponse, PublicAboutPageData>(ABOUT_API.getAboutPage);
};
