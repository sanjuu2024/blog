import request from '@/utils/request';
import type { PrivacyPolicyData, PrivacyPolicyResponse } from '../types/privacy';

const PRIVACY_API = {
	getPrivacyPolicy: 'privacy-policy',
} as const;

// 获取当前隐私政策
export const getPrivacyPolicy = (): Promise<PrivacyPolicyData> => {
	return request.get<PrivacyPolicyResponse, PrivacyPolicyData>(PRIVACY_API.getPrivacyPolicy);
};
