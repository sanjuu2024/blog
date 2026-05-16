<template>
	<el-card class="auth-card">
		<h1 class="mt-2 mb-4 text-center text-2xl">注册</h1>
		<hr class="mb-4 text-gray-300" />
		<el-form
			label-width="auto"
			label-position="top"
			:model="registerForm"
			:rules="rules"
		>
			<el-form-item
				prop="username"
				label="用户名"
			>
				<el-input
					v-model="registerForm.username"
					placeholder="请输入用户名"
				>
					<template #prefix>
						<i-ep-user />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="email"
				label="邮箱"
			>
				<el-input
					v-model="registerForm.email"
					placeholder="请输入邮箱"
				>
					<template #prefix>
						<i-ep-message />
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				prop="password"
				label="密码"
			>
				<el-input
					v-model="registerForm.password"
					placeholder="请输入密码"
					type="password"
					show-password
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
				@click="handlerRegister"
				class="my-6 w-full"
				:disabled="!validated"
			>
				创建账号
			</el-button>
			<div class="links flex justify-between">
				<el-link
					type="primary"
					class="auth-footer-link"
					@click="router.replace('/auth/login')"
				>
					已有帐号？去登录
				</el-link>
				<el-link
					type="primary"
					class="auth-footer-link"
					@click="router.replace('/')"
				>
					返回首页
				</el-link>
			</div>
		</div>
	</el-card>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import type { RegisterRequest } from '../types/auth';
import { register } from '../api/authApi';
import type { FormItemRule } from 'element-plus';

defineOptions({
	name: 'RegisterPage',
});

const router = useRouter();

// 注册表单数据
let registerForm = reactive<RegisterRequest>({
	username: '',
	email: '',
	password: '',
});

// 表单校验规则
const usernamePattern = /^[\p{Script=Han}A-Za-z0-9_-]{4,20}$/u;
const emailPattern =
	/^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/;
const passwordPattern = /^[\p{Script=Han}A-Za-z0-9_!@#$%^&*()+=[\]{}:;'",.?/~`|\\<>-]{6,20}$/u;

let usernameAvailable = ref<boolean>(false);
let emailAvailable = ref<boolean>(false);
let passwordValid = ref<boolean>(false);
const rules = {
	username: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				usernameAvailable.value = false;
				if (!usernamePattern.test(value)) {
					callback(new Error('4-20 位，只允许中文、英文、数字、下划线和短横线。'));
				} else {
					usernameAvailable.value = true;
					callback();
				}
			},
		},
	],
	email: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				emailAvailable.value = false;
				if (!emailPattern.test(value)) {
					callback(new Error('请输入有效的邮箱地址。'));
				} else {
					emailAvailable.value = true;
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

// 控制注册按钮是否可用
let validated = ref<boolean>(false);

// 监听并更新按钮是否可用
watch(
	() => [usernameAvailable.value, emailAvailable.value, passwordValid.value],
	() => {
		validated.value = usernameAvailable.value && emailAvailable.value && passwordValid.value;
	},
);

// 注册
async function handlerRegister() {
	try {
		await register(registerForm);
		router.replace('/auth/login');
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
