<template>
	<el-card class="auth-card">
		<h1 class="mt-2 mb-4 text-center text-2xl">登录</h1>
		<hr class="mb-4 text-gray-300" />
		<el-form label-width="auto" label-position="top" :model="loginForm" :rules="rules">
			<el-form-item prop="account" label="用户名 / 邮箱号">
				<el-input v-model="loginForm.account" placeholder="请输入用户名 / 邮箱号">
					<template #prefix>
						<i-ep-user />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item prop="password" label="密码">
				<el-input v-model="loginForm.password" placeholder="请输入密码" type="password">
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
					@click="router.push('/auth/register')"
					>没有账号？立即注册</el-link
				>
				<el-link type="primary" class="auth-footer-link" @click="router.push('/')"
					>返回首页</el-link
				>
			</div>
		</div>
	</el-card>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import type { LoginRequest } from '../types/auth';
import { login } from '../api/authApi';
import { AxiosError } from 'axios';
import type { FormItemRule } from 'element-plus';

defineOptions({
	name: 'LoginPage',
});

const router = useRouter();

// 登录表单数据
let loginForm = reactive<LoginRequest>({
	account: '',
	password: '',
});

// 表单校验规则
const usernamePattern = /^[\p{Script=Han}A-Za-z0-9_-]{4,20}$/u;
const emailPattern =
	/^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/;
const accountPattern = new RegExp(`(${usernamePattern.source})|(${emailPattern.source})`);
const passwordPattern = /^[\p{Script=Han}A-Za-z0-9_!@#$%^&*()+=[\]{}:;'",.?/~`|\\<>-]{6,20}$/u;

let accountAvailable = ref<boolean>(false);
let passwordValid = ref<boolean>(false);
const rules = {
	account: [
		{
			required: true,
			trigger: 'blur',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				accountAvailable.value = false;
				if (!accountPattern.test(value)) {
					callback(new Error('请输入有效的用户名或邮箱地址。'));
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
			trigger: 'blur',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				passwordValid.value = false;
				if (!passwordPattern.test(value)) {
					callback(
						new Error(
							'6-20 位，允许中文、英文、数字、下划线、短横线和常用 ASCII 特殊字符，不允许空格。',
						),
					);
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
		await login(loginForm);
	} catch (err) {
		console.log((err as AxiosError).response?.data);
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
