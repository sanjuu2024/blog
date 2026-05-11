import { ElMessage } from 'element-plus';

// 后续如果要优化错误消息提示 UI，就可以直接在这里统一修改
export function showErrorMessage(message: string) {
	ElMessage.error(message);
}
