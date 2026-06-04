export const CATEGORY_LEVEL = {
	FIRST: 1,
	SECOND: 2,
} as const;

export type CategoryLevel = (typeof CATEGORY_LEVEL)[keyof typeof CATEGORY_LEVEL];

export const CATEGORY_STATUS = {
	ENABLED: 'ENABLED',
	DISABLED: 'DISABLED',
} as const;

export type CategoryStatus = (typeof CATEGORY_STATUS)[keyof typeof CATEGORY_STATUS];
