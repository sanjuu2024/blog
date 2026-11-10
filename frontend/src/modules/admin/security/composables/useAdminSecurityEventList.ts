import { reactive, ref } from 'vue';
import { listSecurityEvents } from '../api/adminSecurityEventApi';
import type {
	SecurityEventFilterForm,
	SecurityEventItem,
	SecurityEventListQuery,
	SecurityEventPageData,
} from '../types/adminSecurityEvent';

export function useAdminSecurityEventList() {
	let requestId = 0;
	const securityEventList = ref<SecurityEventItem[]>([]);
	const loading = ref(false);
	const pageMeta = reactive({ total: 0, totalPages: 0, hasNext: false });
	const pageParams = reactive({ pageNum: 1, pageSize: 10 });
	const initFilterForm: SecurityEventFilterForm = {
		eventType: '',
		outcome: '',
		userId: undefined,
		createdAtRange: [],
	};
	const filterForm = reactive<SecurityEventFilterForm>({ ...initFilterForm });

	function buildListQuery(): SecurityEventListQuery {
		const [createdAtFrom, createdAtTo] = filterForm.createdAtRange ?? [];
		return {
			pageNum: pageParams.pageNum,
			pageSize: pageParams.pageSize,
			eventType: filterForm.eventType || undefined,
			outcome: filterForm.outcome || undefined,
			userId: filterForm.userId,
			createdAtFrom,
			createdAtTo,
		};
	}

	async function getSecurityEventList(page: number = pageParams.pageNum) {
		const currentRequestId = ++requestId;
		pageParams.pageNum = page;
		loading.value = true;
		try {
			const data: SecurityEventPageData = await listSecurityEvents(buildListQuery());
			if (currentRequestId !== requestId) return;
			securityEventList.value = data.records;
			pageParams.pageNum = data.pageNum;
			pageParams.pageSize = data.pageSize;
			pageMeta.total = data.total;
			pageMeta.totalPages = data.totalPages;
			pageMeta.hasNext = data.hasNext;
		} finally {
			if (currentRequestId === requestId) loading.value = false;
		}
	}

	function resetFilterForm() {
		Object.assign(filterForm, initFilterForm);
		getSecurityEventList(1);
	}

	return {
		securityEventList,
		loading,
		pageMeta,
		pageParams,
		filterForm,
		getSecurityEventList,
		resetFilterForm,
	};
}
