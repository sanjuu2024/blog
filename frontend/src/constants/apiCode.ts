// 运行时存在的对象，不能直接用 type 定义，否则编译后就没了
export const ApiCode = {
	// 通用
	SUCCESS: 0, // HTTP 200，成功
	PARAM_INVALID: 199001, // HTTP 400，请求参数不合法
	RESOURCE_NOT_FOUND: 199404, // HTTP 404，请求资源不存在
	SYSTEM_ERROR: 299001, // HTTP 500，系统内部异常

	// auth 模块 01xxxx
	ACCESS_TOKEN_INVALID: 101001, // HTTP 401，未登录或 Access Token 无效
	ACCESS_TOKEN_EXPIRED: 101002, // HTTP 401，Access Token 已过期
	NO_PERMISSION: 101003, // HTTP 403，无权限访问
	REFRESH_TOKEN_INVALID_OR_EXPIRED: 101004, // HTTP 401，Refresh Token 无效或已过期

	// user 模块 02xxxx
	USER_NOT_FOUND: 102001, // HTTP 404，用户不存在
	USERNAME_EXISTS: 102002, // HTTP 409，用户名已存在
	PASSWORD_ERROR: 102003, // HTTP 401，账号或密码错误
	EMAIL_EXISTS: 102004, // HTTP 409，邮箱已存在
	USER_DISABLED: 102005, // HTTP 403，用户已被禁用
	OLD_PASSWORD_ERROR: 102006, // HTTP 400，原密码错误
	SELF_ROLE_CHANGE_NOT_ALLOWED: 102007, // HTTP 403，当前用户不允许修改自己的角色
	SELF_STATUS_CHANGE_NOT_ALLOWED: 102008, // HTTP 403，当前用户不允许修改自己的状态

	// category 模块 04xxxx
	CATEGORY_HAS_ARTICLES: 104001, // HTTP 409，分类下存在文章，不能删除
} as const;

// 定义 ApiCode 的类型，取 ApiCode 对象的所有属性值组成的联合类型，编译时存在（运行就没了，只是方便写代码、类型检查）
export type ApiCode = (typeof ApiCode)[keyof typeof ApiCode];
