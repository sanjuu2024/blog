export const USERNAME_FORMAT_PATTERN = /^[A-Za-z0-9_-]{2,20}$/;

export const USERNAME_FORMAT_MESSAGE = '2-20 位，只允许英文字母、数字、下划线和短横线。';

export const EMAIL_FORMAT_PATTERN =
	/^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/;

export const EMAIL_FORMAT_MESSAGE = '请输入有效的邮箱地址。';

export const ACCOUNT_FORMAT_PATTERN = new RegExp(
	`(${USERNAME_FORMAT_PATTERN.source})|(${EMAIL_FORMAT_PATTERN.source})`,
	'u',
);

export const ACCOUNT_FORMAT_MESSAGE = '请输入有效的用户名或邮箱地址。';

export const PASSWORD_FORMAT_PATTERN = /^[A-Za-z0-9_!@#$%^&*()+=[\]{}:;'",.?/~`|\\<>-]{6,32}$/;

export const PASSWORD_FORMAT_MESSAGE =
	'6-32 位，允许英文字母、数字、下划线、短横线和常用 ASCII 特殊字符，不允许中文或空白字符。';
