import { listAuditLogs } from '../api/adminAuditApi';
import type {
	AdminAuditLogFilterForm,
	AdminAuditLogItem,
	AdminAuditLogListQuery,
	AdminAuditLogPageData,
} from '../types/adminAudit';

export function useAdminAuditLogList() {
	// 请求序号用于忽略晚返回的旧请求，防止快速筛选时旧数据覆盖新数据。
	let requestId = 0;
	const auditLogList = ref<AdminAuditLogItem[]>([]);
	const loading = ref(false);
	const pageMeta = reactive({ total: 0, totalPages: 0, hasNext: false });
	const pageParams = reactive({ pageNum: 1, pageSize: 10 });
	const initFilterForm: AdminAuditLogFilterForm = {
		operatorId: undefined,
		resourceType: '',
		resourceId: '',
		action: '',
		result: '',
		createdAtRange: [],
	};
	const filterForm = reactive<AdminAuditLogFilterForm>({ ...initFilterForm });

	// 把页面表单转换为接口参数，空字符串不发送给后端。
	function buildAuditLogListQuery(): AdminAuditLogListQuery {
		const [createdAtFrom, createdAtTo] = filterForm.createdAtRange ?? [];
		return {
			pageNum: pageParams.pageNum,
			pageSize: pageParams.pageSize,
			operatorId: filterForm.operatorId,
			resourceType: filterForm.resourceType || undefined,
			resourceId: filterForm.resourceId?.trim() || undefined,
			action: filterForm.action || undefined,
			result: filterForm.result || undefined,
			createdAtFrom,
			createdAtTo,
		};
	}

	// 获取审计日志列表，并以服务端返回的分页数据校准当前页。
	async function getAuditLogList(page: number = pageParams.pageNum) {
		const currentRequestId = ++requestId;
		pageParams.pageNum = page;
		loading.value = true;
		try {
			const data: AdminAuditLogPageData = await listAuditLogs(buildAuditLogListQuery());
			if (currentRequestId !== requestId) return;

			auditLogList.value = data.records;
			pageParams.pageNum = data.pageNum;
			pageParams.pageSize = data.pageSize;
			pageMeta.total = data.total;
			pageMeta.totalPages = data.totalPages;
			pageMeta.hasNext = data.hasNext;
		} catch {
			// 请求错误由统一响应拦截器提示。
		} finally {
			if (currentRequestId === requestId) loading.value = false;
		}
	}

	// 清空全部筛选条件，并重新请求第一页。
	function resetFilterForm() {
		Object.assign(filterForm, initFilterForm);
		getAuditLogList(1);
	}

	return {
		auditLogList,
		loading,
		pageMeta,
		pageParams,
		filterForm,
		getAuditLogList,
		resetFilterForm,
	};
}
