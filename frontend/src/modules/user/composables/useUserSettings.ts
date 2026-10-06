import { ElMessage, type FormInstance, type FormItemRule } from 'element-plus';
import { computed, nextTick, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useIntervalFn } from '@vueuse/core';
import {
	EMAIL_FORMAT_MESSAGE,
	EMAIL_FORMAT_PATTERN,
	EMAIL_VERIFICATION_CODE_MESSAGE,
	EMAIL_VERIFICATION_CODE_PATTERN,
	PASSWORD_FORMAT_MESSAGE,
	PASSWORD_FORMAT_PATTERN,
} from '@/constants/validation';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';
import { AVATAR_MAX_SIZE, validateImageFile } from '@/modules/file/utils/image';
import {
	changeCurrentUserPassword,
	changeCurrentUserEmail,
	getCurrentUserProfile,
	sendEmailChangeCode,
	updateCurrentUserAvatar,
	updateCurrentUserProfile,
} from '../api/userApi';
import {
	USER_ROLE,
	USER_STATUS,
	type ChangeCurrentUserPasswordRequest,
	type ChangeCurrentUserEmailRequest,
	type CurrentUserInfo,
	type UpdateCurrentUserProfileRequest,
	type UpdatedCurrentUserProfileData,
} from '../types/user';

export function useUserSettings() {
	const router = useRouter();
	const authStore = useAuthStore();
	const userStore = useUserStore();

	// 当前用户信息默认值
	const initUserProfile: CurrentUserInfo = {
		id: 0,
		username: '',
		nickname: '',
		email: '',
		role: USER_ROLE.USER,
		status: USER_STATUS.DISABLED,
		avatarUrl: '',
		bio: '',
		lastLoginAt: null,
		createdAt: '',
	};

	const userProfile = reactive<CurrentUserInfo>({ ...initUserProfile });

	// 公开资料表单：P0 只允许修改昵称和简介
	const profileForm = reactive<UpdateCurrentUserProfileRequest>({
		nickname: '',
		bio: '',
	});

	const passwordForm = reactive<ChangeCurrentUserPasswordRequest & { confirmPassword: string }>({
		oldPassword: '',
		newPassword: '',
		confirmPassword: '',
	});

	const emailForm = reactive<ChangeCurrentUserEmailRequest>({
		currentPassword: '',
		newEmail: '',
		verificationCode: '',
	});

	const profileFormRef = ref<FormInstance>();
	const passwordFormRef = ref<FormInstance>();
	const emailFormRef = ref<FormInstance>();
	const profileLoading = ref(false);
	const profileLoaded = ref(false);
	const profileLoadFailed = ref(false);
	const avatarUploading = ref(false);
	const profileSubmitting = ref(false);
	const passwordSubmitting = ref(false);
	const emailCodeSending = ref(false);
	const emailSubmitting = ref(false);
	const emailCodeRemainingSeconds = ref(0);
	const { pause: pauseEmailCodeCountdown, resume: resumeEmailCodeCountdown } = useIntervalFn(
		() => {
			if (emailCodeRemainingSeconds.value <= 1) {
				emailCodeRemainingSeconds.value = 0;
				pauseEmailCodeCountdown();
				return;
			}
			emailCodeRemainingSeconds.value -= 1;
		},
		1000,
		{ immediate: false },
	);
	const profileChanged = computed(() => {
		return (
			(profileForm.nickname ?? '') !== userProfile.nickname ||
			(profileForm.bio ?? '') !== userProfile.bio
		);
	});

	function isValidNickname(nickname: string) {
		return Boolean(nickname.trim()) && nickname.length <= 20;
	}

	function isValidBio(bio: string) {
		return bio.length <= 100;
	}

	function isValidPassword(password: string) {
		return PASSWORD_FORMAT_PATTERN.test(password);
	}

	function isValidNewPassword(newPassword: string) {
		return isValidPassword(newPassword) && newPassword !== passwordForm.oldPassword;
	}

	function isValidConfirmPassword(confirmPassword: string) {
		return Boolean(confirmPassword) && confirmPassword === passwordForm.newPassword;
	}

	const profileValidated = computed(() => {
		const nickname = profileForm.nickname ?? '';
		const bio = profileForm.bio ?? '';
		return isValidNickname(nickname) && isValidBio(bio);
	});
	const passwordValidated = computed(
		() =>
			isValidPassword(passwordForm.oldPassword) &&
			isValidNewPassword(passwordForm.newPassword) &&
			isValidConfirmPassword(passwordForm.confirmPassword),
	);
	const emailTargetValidated = computed(
		() =>
			EMAIL_FORMAT_PATTERN.test(emailForm.newEmail) &&
			emailForm.newEmail.trim().toLowerCase() !== userProfile.email.trim().toLowerCase(),
	);
	const emailValidated = computed(
		() =>
			PASSWORD_FORMAT_PATTERN.test(emailForm.currentPassword) &&
			EMAIL_FORMAT_PATTERN.test(emailForm.newEmail) &&
			emailForm.newEmail.trim().toLowerCase() !== userProfile.email.trim().toLowerCase() &&
			EMAIL_VERIFICATION_CODE_PATTERN.test(emailForm.verificationCode),
	);

	function isFormInstance(formInstance: unknown): formInstance is FormInstance {
		return Boolean(
			typeof formInstance === 'object' &&
			formInstance &&
			'validate' in formInstance &&
			'clearValidate' in formInstance,
		);
	}

	function setProfileFormRef(formInstance: unknown) {
		profileFormRef.value = isFormInstance(formInstance) ? formInstance : undefined;
	}

	function setPasswordFormRef(formInstance: unknown) {
		passwordFormRef.value = isFormInstance(formInstance) ? formInstance : undefined;
	}

	function setEmailFormRef(formInstance: unknown) {
		emailFormRef.value = isFormInstance(formInstance) ? formInstance : undefined;
	}

	function validatePasswordField(prop: keyof typeof passwordForm) {
		void passwordFormRef.value?.validateField(prop).catch(() => undefined);
	}

	const profileRules = {
		nickname: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					const nickname = value ?? '';
					if (!nickname.trim()) {
						callback(new Error('昵称不能为空。'));
					} else if (!isValidNickname(nickname)) {
						callback(new Error('昵称长度不能超过 20 个字符。'));
					} else {
						callback();
					}
				},
			},
		],
		bio: [
			{
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					const bio = value ?? '';
					if (!isValidBio(bio)) {
						callback(new Error('个人简介长度不能超过 100 个字符。'));
					} else {
						callback();
					}
				},
			},
		],
	};

	const passwordRules = {
		oldPassword: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					if (!isValidPassword(value)) {
						callback(new Error(PASSWORD_FORMAT_MESSAGE));
					} else {
						callback();
					}
				},
			},
		],
		newPassword: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					if (!isValidPassword(value)) {
						callback(new Error(PASSWORD_FORMAT_MESSAGE));
					} else if (!isValidNewPassword(value)) {
						callback(new Error('新密码不能与当前密码相同。'));
					} else {
						callback();
					}
				},
			},
		],
		confirmPassword: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					if (!value) {
						callback(new Error('请再次输入新密码。'));
					} else if (!isValidConfirmPassword(value)) {
						callback(new Error('两次输入的新密码不一致。'));
					} else {
						callback();
					}
				},
			},
		],
	};

	const emailRules = {
		currentPassword: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					if (!PASSWORD_FORMAT_PATTERN.test(value ?? ''))
						callback(new Error(PASSWORD_FORMAT_MESSAGE));
					else callback();
				},
			},
		],
		newEmail: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					const email = value ?? '';
					if (!EMAIL_FORMAT_PATTERN.test(email))
						callback(new Error(EMAIL_FORMAT_MESSAGE));
					else if (
						email.trim().toLowerCase() === userProfile.email.trim().toLowerCase()
					) {
						callback(new Error('新邮箱不能与当前邮箱相同。'));
					} else callback();
				},
			},
		],
		verificationCode: [
			{
				required: true,
				trigger: 'change',
				validator: (
					_rule: FormItemRule,
					value: string,
					callback: (error?: Error) => void,
				) => {
					if (!EMAIL_VERIFICATION_CODE_PATTERN.test(value ?? '')) {
						callback(new Error(EMAIL_VERIFICATION_CODE_MESSAGE));
					} else callback();
				},
			},
		],
	};

	watch(
		() => passwordForm.oldPassword,
		() => {
			if (passwordForm.newPassword) {
				validatePasswordField('newPassword');
			}
		},
	);

	watch(
		() => emailForm.newEmail,
		() => {
			emailForm.verificationCode = '';
			emailCodeRemainingSeconds.value = 0;
			pauseEmailCodeCountdown();
		},
	);

	watch(
		() => passwordForm.newPassword,
		() => {
			if (passwordForm.newPassword) {
				validatePasswordField('newPassword');
			}
			if (passwordForm.confirmPassword) {
				validatePasswordField('confirmPassword');
			}
		},
	);

	function setProfileForm() {
		profileForm.nickname = userProfile.nickname;
		profileForm.bio = userProfile.bio;
		nextTick(() => profileFormRef.value?.clearValidate());
	}

	function resetPasswordForm() {
		passwordForm.oldPassword = '';
		passwordForm.newPassword = '';
		passwordForm.confirmPassword = '';
		passwordFormRef.value?.clearValidate();
	}

	function resetEmailForm() {
		emailForm.currentPassword = '';
		emailForm.newEmail = '';
		emailForm.verificationCode = '';
		emailCodeRemainingSeconds.value = 0;
		pauseEmailCodeCountdown();
		emailFormRef.value?.clearValidate();
	}

	async function getUserSettingsProfile() {
		if (profileLoading.value) return;

		profileLoading.value = true;
		profileLoadFailed.value = false;
		try {
			const data = await getCurrentUserProfile();
			Object.assign(userProfile, data);
			setProfileForm();
			profileLoaded.value = true;
		} catch {
			profileLoadFailed.value = true;
		} finally {
			profileLoading.value = false;
		}
	}

	async function updateUserSettingsProfile() {
		if (profileSubmitting.value) return;
		if (!profileFormRef.value) {
			ElMessage.error('公开资料表单未准备好，请稍后再试');
			return;
		}

		profileSubmitting.value = true;
		try {
			await profileFormRef.value.validate();
			const data: UpdatedCurrentUserProfileData = await updateCurrentUserProfile(profileForm);
			Object.assign(userProfile, data);
			userStore.setUserInfo({ ...userProfile });
			setProfileForm();
			ElMessage.success('公开资料更新成功');
		} catch {
			// 表单校验错误由 el-form 展示；请求错误由 request 响应拦截器统一处理
		} finally {
			profileSubmitting.value = false;
		}
	}

	async function updateUserSettingsAvatar(file: File) {
		if (avatarUploading.value) return false;

		const validationMessage = validateImageFile(file, AVATAR_MAX_SIZE);
		if (validationMessage) {
			ElMessage.warning(validationMessage);
			return false;
		}

		avatarUploading.value = true;
		try {
			const data = await updateCurrentUserAvatar(file);
			userProfile.avatarUrl = data.avatarUrl;
			userStore.setUserInfo({ ...userProfile });
			ElMessage.success('头像更新成功');
			return true;
		} catch {
			// 请求错误由 request 响应拦截器统一处理
			return false;
		} finally {
			avatarUploading.value = false;
		}
	}

	async function changeUserSettingsPassword() {
		if (passwordSubmitting.value) return;
		if (!passwordFormRef.value) {
			ElMessage.error('密码表单未准备好，请稍后再试');
			return;
		}

		passwordSubmitting.value = true;
		try {
			await passwordFormRef.value.validate();
			await changeCurrentUserPassword({
				oldPassword: passwordForm.oldPassword,
				newPassword: passwordForm.newPassword,
			});
			ElMessage.success('密码修改成功，请重新登录');
			authStore.clearAuth();
			await router.replace('/auth/login');
		} catch {
			// 表单校验错误由 el-form 展示；请求错误由 request 响应拦截器统一处理
		} finally {
			passwordSubmitting.value = false;
		}
	}

	async function sendUserEmailChangeCode() {
		if (
			emailCodeSending.value ||
			emailCodeRemainingSeconds.value > 0 ||
			!emailTargetValidated.value
		)
			return;

		emailCodeSending.value = true;
		try {
			await sendEmailChangeCode({ email: emailForm.newEmail });
			emailCodeRemainingSeconds.value = 60;
			resumeEmailCodeCountdown();
			ElMessage.success('验证码已发送，请查收邮件');
		} catch {
			// 错误提示由 request 响应拦截器统一处理
		} finally {
			emailCodeSending.value = false;
		}
	}

	async function changeUserSettingsEmail() {
		if (emailSubmitting.value || !emailFormRef.value) return;

		emailSubmitting.value = true;
		try {
			await emailFormRef.value.validate();
			await changeCurrentUserEmail(emailForm);
			ElMessage.success('邮箱修改成功，请重新登录');
			authStore.clearAuth();
			await router.replace('/auth/login');
		} catch {
			// 表单校验错误由 el-form 展示；请求错误由 request 响应拦截器统一处理
		} finally {
			emailSubmitting.value = false;
		}
	}

	return {
		userProfile,
		profileForm,
		passwordForm,
		emailForm,
		profileRules,
		passwordRules,
		emailRules,
		profileValidated,
		profileChanged,
		passwordValidated,
		emailValidated,
		emailTargetValidated,
		profileLoading,
		profileLoaded,
		profileLoadFailed,
		avatarUploading,
		profileSubmitting,
		passwordSubmitting,
		emailCodeSending,
		emailSubmitting,
		emailCodeRemainingSeconds,
		setProfileFormRef,
		setPasswordFormRef,
		setEmailFormRef,
		getUserSettingsProfile,
		updateUserSettingsAvatar,
		updateUserSettingsProfile,
		changeUserSettingsPassword,
		sendUserEmailChangeCode,
		changeUserSettingsEmail,
		resetPasswordForm,
		resetEmailForm,
	};
}
