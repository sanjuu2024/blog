import { ElMessage, type FormInstance } from 'element-plus';
import { createAdminFriendLink, updateAdminFriendLink } from '../api/adminFriendLinkApi';
import {
	FRIEND_LINK_SECTION,
	FRIEND_LINK_STATUS,
	type AdminFriendLinkItem,
	type FriendLinkUpsertRequest,
} from '../types/adminFriendLink';

const SORT_NO_DEFAULT = 100;

export function useAdminFriendLinkForm() {
	const drawerVisible = ref(false);
	const drawerMode = ref<'create' | 'edit'>('create');
	const editingId = ref<number | null>(null);
	const submitting = ref(false);
	const formRef = ref<FormInstance>();
	const initialForm: FriendLinkUpsertRequest = {
		name: '',
		url: '',
		logoUrl: '',
		description: '',
		section: FRIEND_LINK_SECTION.FRIENDS,
		sortNo: SORT_NO_DEFAULT,
		status: FRIEND_LINK_STATUS.ENABLED,
	};
	const form = reactive<FriendLinkUpsertRequest>({ ...initialForm });
	const validateHttpUrl = (
		_rule: unknown,
		value: string | undefined,
		callback: (error?: Error) => void,
	) => {
		const rawValue = value?.trim() ?? '';
		if (!rawValue) {
			callback();
			return;
		}
		try {
			const parsed = new URL(rawValue);
			if (
				(parsed.protocol !== 'http:' && parsed.protocol !== 'https:') ||
				!parsed.hostname ||
				parsed.username ||
				parsed.password
			) {
				callback(new Error('请输入有效的 HTTP(S) 地址'));
				return;
			}
			callback();
		} catch {
			callback(new Error('请输入有效的 HTTP(S) 地址'));
		}
	};
	const rules = {
		name: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
		url: [
			{ required: true, message: '请输入网站地址', trigger: 'blur' },
			{ validator: validateHttpUrl, trigger: 'blur' },
		],
		logoUrl: [{ validator: validateHttpUrl, trigger: 'blur' }],
		section: [{ required: true, message: '请选择友链分区', trigger: 'change' }],
	};

	function setFormRef(value: unknown) {
		formRef.value =
			value && typeof value === 'object' && 'validate' in value
				? (value as FormInstance)
				: undefined;
	}

	function openCreateDrawer() {
		drawerMode.value = 'create';
		editingId.value = null;
		Object.assign(form, initialForm);
		drawerVisible.value = true;
	}

	function openEditDrawer(friendLink: AdminFriendLinkItem) {
		drawerMode.value = 'edit';
		editingId.value = friendLink.id;
		Object.assign(form, {
			name: friendLink.name,
			url: friendLink.url,
			logoUrl: friendLink.logoUrl,
			description: friendLink.description,
			section: friendLink.section,
			sortNo: friendLink.sortNo,
			status: friendLink.status,
		});
		drawerVisible.value = true;
	}

	async function submitForm() {
		const valid = await formRef.value?.validate().catch(() => false);
		if (!valid) return false;
		submitting.value = true;
		try {
			const payload = {
				...form,
				name: form.name.trim(),
				url: form.url.trim(),
				logoUrl: form.logoUrl.trim(),
				description: form.description.trim(),
			};
			if (drawerMode.value === 'create') {
				await createAdminFriendLink(payload);
				ElMessage.success('友链创建成功');
			} else {
				if (editingId.value === null) return false;
				await updateAdminFriendLink(editingId.value, payload);
				ElMessage.success('友链更新成功');
			}
			drawerVisible.value = false;
			return true;
		} catch {
			return false;
		} finally {
			submitting.value = false;
		}
	}

	return {
		drawerVisible,
		drawerMode,
		submitting,
		form,
		rules,
		setFormRef,
		openCreateDrawer,
		openEditDrawer,
		submitForm,
	};
}
