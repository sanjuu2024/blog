<template>
	<el-avatar
		:src="avatarUrl || undefined"
		:size="size"
		:style="{
			backgroundColor: avatarUrl ? undefined : backgroundColor,
			fontSize: `${size / 3}px`,
		}"
		class="user-avatar"
	>
		{{ fallbackText }}
	</el-avatar>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
	avatarUrl?: string | null;
	nickname?: string | null;
	username?: string | null;
	userId?: number | string | null;
	size?: number;
}

const props = withDefaults(defineProps<Props>(), {
	avatarUrl: '',
	nickname: '',
	username: '',
	userId: '',
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
	return props.nickname?.trim() || props.username?.trim() || '用户';
});

const fallbackText = computed(() => {
	const firstChar = displayName.value.charAt(0);

	return /[a-z]/i.test(firstChar) ? firstChar.toUpperCase() : firstChar;
});

const backgroundColor = computed(() => {
	const seed = String(props.userId || props.username || props.nickname || 'default');

	let hash = 0;

	for (const char of seed) {
		hash = char.charCodeAt(0) + ((hash << 5) - hash);
	}

	return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
});
</script>

<style scoped lang="scss">
.user-avatar {
	color: #fff;
	font-weight: bold;
	user-select: none;
}
</style>
