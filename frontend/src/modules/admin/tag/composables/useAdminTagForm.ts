import { ElMessage, type FormInstance } from 'element-plus';
import { type AdminTagItem, type TagUpsertRequest } from '../types/adminTag';
import { createTag, updateTag } from '../api/adminTagApi';
import { TAG_STATUS } from '@/modules/tag/constants/tag';

// 表单校验相关常量
const TAG_NAME_MAX_LENGTH = 50;
const TAG_DESCRIPTION_MAX_LENGTH = 255;

export function useAdminTagForm() {
	// 是否显示新建 / 编辑标签的抽屉
	const showDrawer = ref(false);
	const drawerMode = ref<'create' | 'edit'>('create');

	// 当前正在编辑的 TagId，创建标签时为 null
	const editingTagId = ref<number | null>(null);

	// 抽屉表单引用（用于获取其上的 validate() 函数）
	const tagFormRef = ref<FormInstance>();

	// 抽屉绑定的表单数据和初始值
	const initTagForm: TagUpsertRequest = {
		name: '',
		description: '',
		status: TAG_STATUS.ENABLED,
	};
	const tagForm = reactive<TagUpsertRequest>({ ...initTagForm });

	// 表单校验规则
	const rules = {
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
						callback(new Error('请输入标签名称'));
						return;
					}

					if (name.length > TAG_NAME_MAX_LENGTH) {
						callback(new Error(`标签名称不能超过 ${TAG_NAME_MAX_LENGTH} 个字符`));
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

					if (description.length > TAG_DESCRIPTION_MAX_LENGTH) {
						callback(
							new Error(`标签描述不能超过 ${TAG_DESCRIPTION_MAX_LENGTH} 个字符`),
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
					if (value !== TAG_STATUS.ENABLED && value !== TAG_STATUS.DISABLED) {
						callback(new Error('请选择有效的标签状态'));
						return;
					}

					callback();
				},
			},
		],
	};

	// 点击 新建标签 按钮时调用，重置表单数据并显示抽屉
	function openCreateDrawer() {
		drawerMode.value = 'create';
		editingTagId.value = null;
		Object.assign(tagForm, initTagForm);

		nextTick(() => {
			showDrawer.value = true;
		});
	}

	// 点击标签列表中的 编辑 按钮时调用，重置表单数据并显示抽屉
	function openUpdateDrawer(tag: AdminTagItem) {
		drawerMode.value = 'edit';
		editingTagId.value = tag.id;

		tagForm.name = tag.name;
		tagForm.description = tag.description;
		tagForm.status = tag.status;

		nextTick(() => {
			showDrawer.value = true;
		});
	}

	// 传给 <el-form> 的 ref 属性从而把表单实例绑到 tagFormRef 上的函数；主要用于获取其上的 validate() 函数
	// 注：最好不要直接把 tagFormRef 传给 <el-form> 的 ref，而是像这样封装函数；因为它的类型是 Ref<FormInstance | undefined>，而 <el-form> 组件实例的类型是 FormInstance，ts 报错，容易 bug
	function setTagFormRef(form: unknown) {
		if (form && typeof form === 'object' && 'validate' in form) {
			tagFormRef.value = form as FormInstance;
			return;
		}

		tagFormRef.value = undefined;
	}

	// 发送 创建标签 请求
	async function handleCreateTag(form: TagUpsertRequest) {
		// 不进行 try-catch，错误抛出，由 handleUpsertTag 统一捕获处理

		await createTag(form);

		ElMessage.success(`标签 ${form.name} 创建成功`);
		Object.assign(tagForm, initTagForm); // 重置表单数据（注意是响应式，不能直接赋值否则会断响应式！）
	}

	// 发送 更新标签 请求
	async function handleUpdateTag(form: TagUpsertRequest) {
		// 不进行 try-catch，错误抛出，由 handleUpsertTag 统一捕获处理

		if (editingTagId.value == null) {
			ElMessage.error('当前编辑的标签不存在，请关闭后重新打开编辑');
			throw new Error('Missing editing tag id');
		}

		await updateTag(editingTagId.value, form);
		ElMessage.success(`标签 ${form.name} 更新成功`);
		Object.assign(tagForm, initTagForm); // 重置表单数据（注意是响应式，不能直接赋值否则会断响应式！）
	}

	// 点击抽屉中的“确定”按钮时，先校验表单，校验通过则根据当前抽屉模式（创建/编辑）调用对应的处理函数发送请求，成功后关闭抽屉
	async function handleUpsertTag(form: TagUpsertRequest = tagForm) {
		const valid = await tagFormRef.value?.validate().catch(() => false);
		if (!valid) {
			ElMessage.error('表单参数校验失败，请检查输入');
			return false;
		}

		try {
			if (drawerMode.value === 'create') {
				await handleCreateTag(form);
			} else {
				await handleUpdateTag(form);
			}
			showDrawer.value = false;

			return true;
		} catch {
			return false;
		}
	}

	return {
		showDrawer,
		drawerMode,
		tagForm,
		rules,
		openCreateDrawer,
		openUpdateDrawer,
		setTagFormRef,
		handleUpsertTag,
	};
}
