import { nextTick, reactive, ref } from 'vue';
import { ElMessage, type FormInstance } from 'element-plus';
import { type AdminCategoryItem, type CategoryUpsertRequest } from '../types/adminCategory';
import { createCategory, updateCategory } from '../api/adminCategoryApi';
import { CATEGORY_LEVEL, CATEGORY_STATUS } from '@/modules/category/constants/category';

const CATEGORY_NAME_MAX_LENGTH = 50;
const CATEGORY_DESCRIPTION_MAX_LENGTH = 255;
const CATEGORY_SORT_NO_MIN = 0;
const CATEGORY_SORT_NO_MAX = 999;

export function useAdminCategoryForm() {
	// 是否显示新建 / 编辑分类的抽屉
	const showDrawer = ref(false);
	const drawerMode = ref<'create' | 'edit'>('create');

	// 当前正在编辑的 categoryId，创建分类时为 null
	const editingCategoryId = ref<number | null>(null);

	// 抽屉表单引用（用于获取其上的 validate() 函数）
	const categoryFormRef = ref<FormInstance>();

	// 抽屉绑定的表单数据和初始值
	const initCategoryForm: CategoryUpsertRequest = {
		parentId: null,
		level: CATEGORY_LEVEL.FIRST,
		name: '',
		description: '',
		sortNo: 100,
		status: CATEGORY_STATUS.ENABLED,
	};
	const categoryForm = reactive<CategoryUpsertRequest>({ ...initCategoryForm });

	// 抽屉表单校验规则
	const rules = {
		level: [
			{
				trigger: 'change',
				required: true,
				validator: (
					_rule: unknown,
					value: number | undefined,
					callback: (error?: Error) => void,
				) => {
					if (value !== CATEGORY_LEVEL.FIRST && value !== CATEGORY_LEVEL.SECOND) {
						callback(new Error('请选择有效的分类级别'));
						return;
					}

					callback();
				},
			},
		],
		parentId: [
			{
				trigger: 'change',
				validator: (
					_rule: unknown,
					value: number | string | null | undefined,
					callback: (error?: Error) => void,
				) => {
					if (categoryForm.level === CATEGORY_LEVEL.FIRST) {
						callback();
						return;
					}

					if (value == null || value === '') {
						callback(new Error('请选择父级分类'));
						return;
					}

					if (!Number.isInteger(value) || Number(value) <= 0) {
						callback(new Error('父级分类不合法'));
						return;
					}

					callback();
				},
			},
		],
		name: [
			{
				trigger: 'blur',
				required: true,
				validator: (
					_rule: unknown,
					value: string | undefined,
					callback: (error?: Error) => void,
				) => {
					const name = value?.trim() ?? '';

					if (!name) {
						callback(new Error('请输入分类名称'));
						return;
					}

					if (name.length > CATEGORY_NAME_MAX_LENGTH) {
						callback(new Error(`分类名称不能超过 ${CATEGORY_NAME_MAX_LENGTH} 个字符`));
						return;
					}

					callback();
				},
			},
		],
		description: [
			{
				trigger: 'blur',
				validator: (
					_rule: unknown,
					value: string | undefined,
					callback: (error?: Error) => void,
				) => {
					const description = value?.trim() ?? '';

					if (description.length > CATEGORY_DESCRIPTION_MAX_LENGTH) {
						callback(
							new Error(`分类描述不能超过 ${CATEGORY_DESCRIPTION_MAX_LENGTH} 个字符`),
						);
						return;
					}

					callback();
				},
			},
		],
		sortNo: [
			{
				trigger: 'blur',
				validator: (
					_rule: unknown,
					value: number | undefined,
					callback: (error?: Error) => void,
				) => {
					if (value == null) {
						callback();
						return;
					}

					if (
						!Number.isInteger(value) ||
						value < CATEGORY_SORT_NO_MIN ||
						value > CATEGORY_SORT_NO_MAX
					) {
						callback(
							new Error(
								`排序值必须是 ${CATEGORY_SORT_NO_MIN} 到 ${CATEGORY_SORT_NO_MAX} 之间的整数`,
							),
						);
						return;
					}

					callback();
				},
			},
		],
		status: [
			{
				trigger: 'change',
				required: true,
				validator: (
					_rule: unknown,
					value: string | undefined,
					callback: (error?: Error) => void,
				) => {
					if (value !== CATEGORY_STATUS.ENABLED && value !== CATEGORY_STATUS.DISABLED) {
						callback(new Error('请选择有效的分类状态'));
						return;
					}

					callback();
				},
			},
		],
	};

	// 构建创建/更新分类的请求体
	function buildCategoryPayload(form: CategoryUpsertRequest): CategoryUpsertRequest {
		return {
			parentId: form.level === CATEGORY_LEVEL.FIRST ? null : form.parentId,
			level: form.level,
			name: form.name.trim(),
			description: form.description?.trim() ?? '',
			sortNo: form.sortNo,
			status: form.status,
		};
	}

	// 点击页面中的“新建分类”按钮时，显示抽屉，并重置表单数据为初始值
	function openCreateDrawer() {
		drawerMode.value = 'create';
		editingCategoryId.value = null;
		Object.assign(categoryForm, initCategoryForm);

		nextTick(() => {
			showDrawer.value = true;
		});
	}

	// 点击分类表格中某一行分类的“编辑”按钮时，显示抽屉，并将该分类的数据填充到表单中
	function openUpdateDrawer(category: AdminCategoryItem) {
		drawerMode.value = 'edit';
		editingCategoryId.value = category.id;
		Object.assign(categoryForm, {
			parentId: category.parentId,
			level: category.level,
			name: category.name,
			description: category.description,
			sortNo: category.sortNo,
			status: category.status,
		});

		nextTick(() => {
			showDrawer.value = true;
		});
	}

	// 发送 创建分类 请求
	async function handleCreateCategory(form: CategoryUpsertRequest) {
		// 不进行 try-catch，错误抛出，由 handleUpsertCategory 统一捕获处理

		const payload = buildCategoryPayload(form);
		const name = payload.name;

		await createCategory(payload);
		Object.assign(categoryForm, initCategoryForm); // 重置表单数据（注意是响应式，不能直接赋值否则会断响应式！）
		showDrawer.value = false;

		ElMessage.success(`分类 ${name} 创建成功`);
	}

	// 发送 更新分类 请求
	async function handleUpdateCategory(form: CategoryUpsertRequest) {
		// 不进行 try-catch，错误抛出，由 handleUpsertCategory 统一捕获处理

		if (editingCategoryId.value == null) {
			ElMessage.error('当前编辑的分类不存在，请关闭后重新打开编辑');
			throw new Error('Missing editing category id');
		}

		const payload = buildCategoryPayload(form);
		const name = payload.name;
		await updateCategory(editingCategoryId.value, payload);
		Object.assign(categoryForm, initCategoryForm); // 重置表单数据（注意是响应式，不能直接赋值否则会断响应式！）
		showDrawer.value = false;

		ElMessage.success(`分类 ${name} 更新成功`);
	}

	// 传给 <el-form> 的 ref 属性从而把表单实例绑到 categoryFormRef 上的函数；主要用于获取其上的 validate() 函数
	// 注：最好不要直接把 categoryFormRef 传给 <el-form> 的 ref，而是像这样封装函数；因为它的类型是 Ref<FormInstance | undefined>，而 <el-form> 组件实例的类型是 FormInstance，ts 报错，容易 bug
	function setCategoryFormRef(form: unknown) {
		if (form && typeof form === 'object' && 'validate' in form) {
			categoryFormRef.value = form as FormInstance;
			return;
		}

		categoryFormRef.value = undefined;
	}

	// 点击抽屉中的“确定”按钮时，先校验表单，校验通过则根据当前抽屉模式（创建/编辑）调用对应的处理函数发送请求
	async function handleUpsertCategory(form: CategoryUpsertRequest = categoryForm) {
		const valid = await categoryFormRef.value?.validate().catch(() => false);
		if (!valid) {
			ElMessage.error('表单参数校验失败，请检查输入');
			return false;
		}

		try {
			if (drawerMode.value === 'create') {
				await handleCreateCategory(form);
			} else {
				await handleUpdateCategory(form);
			}

			return true;
		} catch {
			return false;
		}
	}

	return {
		showDrawer,
		drawerMode,
		editingCategoryId,
		initCategoryForm,
		categoryForm,
		rules,
		setCategoryFormRef,
		openCreateDrawer,
		openUpdateDrawer,
		handleUpsertCategory,
	};
}
