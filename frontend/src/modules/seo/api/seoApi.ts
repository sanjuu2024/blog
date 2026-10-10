import request from '@/utils/request';
import type { SeoMetadata, SeoMetadataResponse } from '../types/seo';

// 获取公开页面 SEO 元信息接口
export const getSeoMetadata = (path: string): Promise<SeoMetadata> => {
	// 🔺注意 params 是 query 参数，需要放在 Axios 请求配置中。
	return request.get<SeoMetadataResponse, SeoMetadata>('seo', {
		params: { path },
		meta: { showError: false },
	});
};
