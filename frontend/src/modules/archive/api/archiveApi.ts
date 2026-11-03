import request from '@/utils/request';
import type { ApiResult } from '@/types/api';
import type { ArchiveYear, ArchiveQuery, ArchiveArticlePage } from '../types/archive';

const ARCHIVE_API = {
	getArchiveYears: 'archives/years',
	getArchiveArticles: 'archives/articles',
} as const;

// 获取公开文章年份及完整篇数
export const getArchiveYears = (timeZone: string): Promise<ArchiveYear[]> => {
	return request.get<ApiResult<ArchiveYear[]>, ArchiveYear[]>(ARCHIVE_API.getArchiveYears, {
		params: { timeZone },
	});
};

// 分页获取当前归档条件下的紧凑文章列表
export const getArchiveArticles = (
	params: ArchiveQuery & { timeZone: string; pageNum: number; pageSize: number },
	signal: AbortSignal,
): Promise<ArchiveArticlePage> => {
	return request.get<ApiResult<ArchiveArticlePage>, ArchiveArticlePage>(
		ARCHIVE_API.getArchiveArticles,
		{ params, signal },
	);
};
