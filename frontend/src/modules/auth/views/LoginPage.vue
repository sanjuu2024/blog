<template>
	<el-card class="auth-card">
		<h1 class="mt-2 mb-4 text-center text-2xl">登录</h1>
		<hr class="mb-4 text-gray-300" />
		<el-form
			label-width="auto"
			label-position="top"
			:model="loginForm"
			:rules="rules"
		>
			<el-form-item
				prop="account"
				label="用户名 / 邮箱号"
			>
				<el-input
					v-model="loginForm.account"
					placeholder="请输入用户名 / 邮箱号"
				>
					<template #prefix>
						<i-ep-user />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="password"
				label="密码"
			>
				<el-input
					v-model="loginForm.password"
					placeholder="请输入密码"
					type="password"
				>
					<template #prefix>
						<i-ep-lock />
					</template>
				</el-input>
			</el-form-item>
		</el-form>

		<div class="footer">
			<el-button
				type="primary"
				@click="handlerLogin"
				class="my-4 w-full"
				:disabled="!validated"
			>
				登录
			</el-button>
			<div class="links flex justify-between">
				<el-link
					type="primary"
					class="auth-footer-link"
					@click="router.replace('/auth/register')"
					>没有账号？立即注册
				</el-link>
				<el-link
					type="primary"
					class="auth-footer-link"
					@click="router.replace('/')"
					>返回首页
				</el-link>
			</div>
		</div>
	</el-card>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import type { LoginData, LoginRequest } from '../types/auth';
import { login } from '../api/authApi';
import type { FormItemRule } from 'element-plus';
import {
	ACCOUNT_FORMAT_MESSAGE,
	ACCOUNT_FORMAT_PATTERN,
	PASSWORD_FORMAT_MESSAGE,
	PASSWORD_FORMAT_PATTERN,
} from '@/constants/validation';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';

defineOptions({
	name: 'LoginPage',
});

const router = useRouter();
const route = useRoute();

// 登录表单数据
let loginForm = reactive<LoginRequest>({
	account: '',
	password: '',
});

let accountAvailable = ref<boolean>(false);
let passwordValid = ref<boolean>(false);

// 表单校验规则
const rules = {
	account: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				accountAvailable.value = false;
				if (!ACCOUNT_FORMAT_PATTERN.test(value)) {
					callback(new Error(ACCOUNT_FORMAT_MESSAGE));
				} else {
					accountAvailable.value = true;
					callback();
				}
			},
		},
	],
	password: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				passwordValid.value = false;
				if (!PASSWORD_FORMAT_PATTERN.test(value)) {
					callback(new Error(PASSWORD_FORMAT_MESSAGE));
				} else {
					passwordValid.value = true;
					callback();
				}
			},
		},
	],
};

// 控制登录按钮是否可用
let validated = ref<boolean>(false);

// 监听并更新按钮是否可用
watch(
	() => [accountAvailable.value, passwordValid.value],
	() => {
		validated.value = accountAvailable.value && passwordValid.value;
	},
);

// 登录
async function handlerLogin() {
	try {
		const res: LoginData = await login(loginForm);
		useAuthStore().setAccessToken(res.accessToken);
		const redirectQuery = route.query.redirect;
		const redirect = Array.isArray(redirectQuery) ? redirectQuery[0] : redirectQuery;
		const userStore = useUserStore();
		userStore.setUserInfo(res.user);
		router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/'); // 如果是从其他需要登录的页面跳过来登录页的，登录后跳转回去。
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	}
}
</script>

<style lang="scss" scoped>
.auth-card {
	--el-font-size-base: 1rem;
	:deep(.el-form-item__error) {
		margin-top: 4px;
		margin-left: 2px;
		line-height: 0.8rem;
	}
}

.auth-footer-link {
	--el-link-font-size: 0.8rem;
}
</style>
