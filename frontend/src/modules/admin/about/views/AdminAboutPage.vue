<template>
	<div class="admin-about">
		<el-card class="admin-about__card">
			<template #header>
				<div class="admin-about__header">
					<span class="text-xl font-bold">关于页内容编辑</span>
					<el-button
						type="primary"
						:loading="submitting"
						:disabled="submitting || !hasUnsavedChanges"
						@click="saveAboutPage"
					>
						保存
					</el-button>
				</div>
			</template>

			<div
				v-if="loadFailed"
				class="admin-about__state"
			>
				<p>关于页内容加载失败</p>
				<el-button
					type="primary"
					@click="getAdminAboutPage"
				>
					重试
				</el-button>
			</div>
			<AppLoading
				v-else
				:loading="loading"
			>
				<MdEditor
					v-model="contentMd"
					:theme="resolvedTheme"
					:maxlength="100000"
					show-code-row-number
				/>
			</AppLoading>

			<div
				v-if="aboutPage?.updatedBy"
				class="admin-about__meta"
			>
				最后编辑：{{ aboutPage.updatedBy.username }}
				<span class="mx-2">·</span>
				{{ formatDateTime(aboutPage.updatedAt) }}
			</div>
		</el-card>
	</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { MdEditor } from 'md-editor-v3';
import { useTheme } from '@/composables/useTheme';
import { formatDateTime } from '@/utils/datetime';
import { useAdminAboutPage } from '../composables/useAdminAboutPage';

defineOptions({
	name: 'AdminAboutPage',
});

const { resolvedTheme } = useTheme();
const {
	aboutPage,
	contentMd,
	loading,
	loadFailed,
	submitting,
	hasUnsavedChanges,
	getAdminAboutPage,
	saveAboutPage,
} = useAdminAboutPage();

onMounted(() => {
	getAdminAboutPage();
});

onBeforeRouteLeave(async () => {
	if (!hasUnsavedChanges.value || submitting.value) return true;

	try {
		await ElMessageBox.confirm(
			'确认离开关于页编辑吗？目前修改内容尚未保存，退出后将丢失所有未保存的更改。',
			'退出编辑',
			{
				confirmButtonText: '确定离开',
				cancelButtonText: '取消',
				type: 'warning',
			},
		);
		return true;
	} catch {
		return false;
	}
});
</script>

<style lang="scss" scoped>
.admin-about {
	height: 100%;
	min-height: 0;
}

.admin-about__card {
	min-height: 100%;
	border: none;
	background-color: var(--app-bg);
	box-shadow: none;
}

.admin-about__header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 1rem;
}

.admin-about__state {
	display: flex;
	min-height: 16rem;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 1rem;
	color: var(--app-text-muted);
}

.admin-about__meta {
	margin-top: 1rem;
	color: var(--app-text-muted);
	font-size: 0.85rem;
}
</style>
