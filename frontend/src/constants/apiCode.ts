// 只维护前端需要执行特殊分支的业务码，其余错误统一展示后端返回的 message
export const ApiCode = {
	ACCESS_TOKEN_INVALID: 101001, // HTTP 401，未登录或 Access Token 无效
	ACCESS_TOKEN_EXPIRED: 101002, // HTTP 401，Access Token 已过期
	USER_DISABLED: 102005, // HTTP 403，用户已被禁用
	PRIVACY_POLICY_VERSION_MISMATCH: 102009, // HTTP 409，注册时提交的隐私政策版本已过期
} as const;
