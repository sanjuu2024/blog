import dayjs from 'dayjs';

export const formatDateTime = (
	date: Date | string | null,
	format: string = 'YYYY-MM-DD HH:mm:ss',
) => {
	if (!date) return '-';
	return dayjs(date).format(format);
};
