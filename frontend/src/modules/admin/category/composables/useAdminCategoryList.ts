import { ElMessage } from 'element-plus';
import { deleteCategory, listCategories, updateCategory } from '../api/adminCategoryApi';
import {
	CATEGORY_STATUS,
	type AdminCategoryItem,
	type AdminCategoryListQuery,
	type CategoryStatus,
	type UpdatedCategoryData,
} from '../types/adminCategory';

// 🔺由于筛选条件中分类“状态”单选时选中后除了点击重置按钮之外无法取消、用户体验不好，这里让status可以取值空字符串（即“全部状态”的取值）
type AdminCategoryQueryForm = Omit<AdminCategoryListQuery, 'status'> & {
	status?: CategoryStatus | '';
};

export function useAdminCategoryList() {
	// 分类列表数据
	const categoryList = ref<AdminCategoryItem[]>([]);

	// 查询分类列表条件
	const queryParams = reactive<AdminCategoryQueryForm>({
		keyword: undefined,
		status: '',
		level: undefined,
		parentId: undefined,
	});

	// 构造分类列表查询条件（AdminCategoryQueryForm 转 AdminCategoryListQuery）
	function buildCategoryListQuery(): AdminCategoryListQuery {
		return {
			keyword: queryParams.keyword || undefined,
			status: queryParams.status || undefined,
			level: queryParams.level,
			parentId: queryParams.level === 2 ? queryParams.parentId : undefined, // 只有当查询二级分类时才传 parentId
		};
	}

	// 获取分类列表
	async function getCategoryList() {
		try {
			categoryList.value = await listCategories(buildCategoryListQuery());
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	// 点击 启用/禁用分类 按钮
	async function toggleCategoryStatus(category: AdminCategoryItem) {
		const nextStatus =
			category.status === CATEGORY_STATUS.ENABLED
				? CATEGORY_STATUS.DISABLED
				: CATEGORY_STATUS.ENABLED;

		try {
			const res: UpdatedCategoryData = await updateCategory(category.id, {
				parentId: category.level === 1 ? null : category.parentId, // 一级分类 parentId 固定为 null
				level: category.level,
				name: category.name,
				description: category.description,
				sortNo: category.sortNo,
				status: nextStatus,
			});

			category.status = res.status;
			ElMessage.success(
				`分类 ${category.name} 已${nextStatus === CATEGORY_STATUS.ENABLED ? '启用' : '禁用'}`,
			);
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			return;
		}
	}

	// 点击删除分类按钮
	async function handleDeleteCategory(categoryId: number) {
		try {
			await deleteCategory(categoryId);

			getCategoryList(); // 刷新列表

			ElMessage.success('删除分类成功');
			return true;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
			return false;
		}
	}

	// 重置查询条件
	function resetQueryParams() {
		// 注意是 reactive，不能整个直接重新赋值，会断响应式
		Object.assign(queryParams, {
			keyword: undefined,
			status: '',
			level: undefined,
			parentId: undefined,
		});

		getCategoryList();
	}

	return {
		categoryList,
		queryParams,
		getCategoryList,
		toggleCategoryStatus,
		handleDeleteCategory,
		resetQueryParams,
	};
}
