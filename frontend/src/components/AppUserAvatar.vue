<template>
	<div
		class="avatar-container"
		:style="{ width: `${size}px`, height: `${size}px` }"
	>
		<el-avatar
			:src="avatarUrl || undefined"
			:size="size"
			:style="{
				backgroundColor: avatarUrl || deleted || guest ? 'var(--app-bg)' : backgroundColor,
				fontSize: `${size / 3}px`,
			}"
			class="user-avatar flex items-center justify-center"
		>
			<i-solar-user-bold
				v-if="deleted || guest"
				class="text-lg text-(--app-text-muted-more)"
			/>
			<template v-else>{{ fallbackText }}</template>
		</el-avatar>

		<!-- 管理员标志 -->
		<div
			v-if="isAdmin && !deleted && !guest"
			class="user-avatar-badge"
		>
			<i-lets-icons-lightning-fill class="h-full w-full rotate-12 text-(--app-main)" />
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
	avatarUrl?: string | null;
	name?: string | null;
	userId?: number | string | null;
	deleted?: boolean;
	guest?: boolean;
	isAdmin?: boolean;
	size?: number;
}

const props = withDefaults(defineProps<Props>(), {
	avatarUrl: '',
	name: '',
	userId: '',
	deleted: false,
	guest: false,
	isAdmin: false,
	size: 20,
});

const AVATAR_COLORS = [
	'#FFC1CC',
	'#D0F0C0',
	'#ACE1AF',
	'#F6D155',
	'#F7E98E',
	'#92A8D1',
	'#FBCEB1',
	'#836953',
	'#CFCFC4',
	'#AFEEEE',
	'#F88379',
	'#73C0DE',
	'#C8A2C8',
];

const displayName = computed(() => {
	return props.name?.trim() || '用户';
});

const fallbackText = computed(() => {
	const firstChar = displayName.value.charAt(0);

	return /[a-z]/i.test(firstChar) ? firstChar.toUpperCase() : firstChar;
});

const backgroundColor = computed(() => {
	const seed = String(props.userId || props.name || 'default');

	let hash = 0;

	for (const char of seed) {
		hash = char.charCodeAt(0) + ((hash << 5) - hash);
	}

	return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
});
</script>

<style scoped lang="scss">
.avatar-container {
	position: relative;
}

.user-avatar {
	color: #fff;
	font-weight: bold;
	user-select: none;
	border: 1px solid var(--app-border);
}

.user-avatar-badge {
	position: absolute;
	bottom: 0;
	right: -3px;
	width: 45%;
	height: 45%;
	border-radius: 50%;
	background-color: #fff;
	display: flex;
	justify-content: center;
	align-items: center;
}
</style>
