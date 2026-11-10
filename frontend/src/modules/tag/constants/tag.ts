export const TAG_STATUS = {
	ENABLED: 'ENABLED',
	DISABLED: 'DISABLED',
} as const;

export type TagStatus = (typeof TAG_STATUS)[keyof typeof TAG_STATUS];
