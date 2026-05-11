import type { ApiCode } from '@/constants/apiCode';

// 后端响应体
export interface ApiResult<T = unknown> {
	code: ApiCode;
	message: string;
	data: T;
}

// 分页查询结果
export interface PageResult<T> {
	records: T[];
	pageNum: number;
	pageSize: number;
	total: number;
	totalPages: number;
	hasNext: boolean;
}

// 字段验证错误信息
export interface FieldValidationError {
	field: string;
	message: string;
	rejectedValue: unknown;
}
