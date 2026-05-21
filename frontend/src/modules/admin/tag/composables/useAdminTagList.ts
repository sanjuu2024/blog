import { ElMessage } from 'element-plus';
import { deleteTag, listTags, updateTag } from '../api/adminTagApi';
import {
	TAG_STATUS,
	type AdminTagItem,
	type AdminTagListQuery,
	type TagStatus,
	type TagUpsertRequest,
	type UpdatedTagData,
} from '../types/adminTag';

// 🔺由于筛选条件中分类“状态”单选时选中后除了点击重置按钮之外无法取消、用户体验不好，这里让status可以取值空字符串（即“全部状态”的取值）
type AdminTagQueryForm = Omit<AdminTagListQuery, 'status'> & {
	status?: TagStatus | '';
};

export function useAdminTagList() {
	// 标签列表数据
	const tagList = ref<AdminTagItem[]>([]);

	// 查询标签列表的查询参数
	const queryParams = reactive<AdminTagQueryForm>({
		keyword: '',
		status: '',
	});

	// 构建查询标签请求
	function buildTagQueryParams(): AdminTagListQuery {
		return {
			keyword: queryParams.keyword || undefined,
			status: queryParams.status || undefined,
		};
	}

	// 获取标签列表
	async function getTagList() {
		try {
			const res = await listTags(buildTagQueryParams());
			tagList.value = res;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	// 重置查询参数为初始值并重新获取标签列表
	function resetQueryParams() {
		queryParams.keyword = '';
		queryParams.status = '';
	}

	// 点击标签列表中的删除按钮时调用，传入要删除的标签 ID
	async function handleDeleteTag(tag: AdminTagItem) {
		try {
			const name = tag.name;
			await deleteTag(tag.id);

			ElMessage.success(`标签 ${name} 删除成功`);
			await getTagList();
		} catch (error) {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	// 点击标签列表中的状态切换按钮时调用，传入要切换状态的标签对象
	async function toggleTagStatus(tag: AdminTagItem) {
		try {
			const newStatus =
				tag.status === TAG_STATUS.ENABLED ? TAG_STATUS.DISABLED : TAG_STATUS.ENABLED;
			const res: UpdatedTagData = await updateTag(tag.id, {
				name: tag.name,
				description: tag.description,
				status: newStatus,
			} as TagUpsertRequest);

			tag.status = res.status; // 直接更新状态，不需要重新拉取列表
			ElMessage.success(
				`标签 ${tag.name} 已${newStatus === TAG_STATUS.ENABLED ? '启用' : '禁用'}`,
			);
		} catch (error) {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	return {
		tagList,
		queryParams,
		getTagList,
		resetQueryParams,
		handleDeleteTag,
		toggleTagStatus,
	};
}
