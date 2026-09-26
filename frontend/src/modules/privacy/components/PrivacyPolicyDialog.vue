<template>
	<el-dialog
		v-model="visible"
		title="隐私政策"
		width="min(48rem, calc(100vw - 2rem))"
		@open="getPrivacyPolicyContent"
	>
		<div class="privacy-policy-dialog__body app-scrollbar">
			<AppLoading :loading="loading">
				<div
					v-if="loadFailed"
					class="privacy-policy-dialog__state"
				>
					<p>隐私政策加载失败</p>
					<el-button
						type="primary"
						@click="getPrivacyPolicyContent"
					>
						重试
					</el-button>
				</div>
				<div
					v-else-if="privacyPolicy"
					class="privacy-policy-content article-markdown markdown-body"
					v-html="privacyPolicy.contentHtml"
				></div>
			</AppLoading>
		</div>
		<template #footer>
			<el-button
				type="primary"
				@click="visible = false"
			>
				关闭
			</el-button>
		</template>
	</el-dialog>
</template>

<script setup lang="ts">
import { usePrivacyPolicy } from '../composables/usePrivacyPolicy';

defineOptions({
	name: 'PrivacyPolicyDialog',
});

const visible = defineModel<boolean>({ required: true });

const { privacyPolicy, loading, loadFailed, getPrivacyPolicyContent } = usePrivacyPolicy();
</script>

<style lang="scss" scoped>
.privacy-policy-dialog__body {
	max-height: 65vh;
	overflow-y: auto;
}

.privacy-policy-content {
	padding: 16px;
	line-height: 2rem;
	font-size: small;
}

.privacy-policy-dialog__state {
	display: flex;
	min-height: 10rem;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 1rem;
	color: var(--app-text-muted);
}
</style>
