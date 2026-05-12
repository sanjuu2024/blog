import { ElMessage } from 'element-plus';
import 'element-plus/es/components/message/style/css'; // 如果少了这一句则 ElMessage 缺少样式无法显示

// 后续如果要优化错误消息提示 UI，就可以直接在这里统一修改
export function showErrorMessage(message: string) {
	ElMessage.error(message);
}
