import type { ApiResult } from '@/types/api';

export interface PrivacyPolicyData {
	version: string;
	contentHtml: string;
	contentText: string;
}

export type PrivacyPolicyResponse = ApiResult<PrivacyPolicyData>;
