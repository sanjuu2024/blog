<template>
	<el-popover
		placement="top"
		:width="280"
		:trigger="['hover', 'focus', 'click']"
		:show-after="200"
		:hide-after="50"
	>
		<template #reference>
			<span
				class="guest-user-popover__reference"
				role="button"
				tabindex="0"
				aria-label="查看游客信息"
			>
				<slot></slot>
			</span>
		</template>

		<div class="guest-user-popover__content">
			<AppUserAvatar
				:name="displayName"
				:size="48"
			/>
			<div class="guest-user-popover__identity">
				<strong>{{ displayName }}</strong>
				<span>游客</span>
			</div>
		</div>
	</el-popover>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AppUserAvatar from './AppUserAvatar.vue';

defineOptions({
	name: 'GuestUserPopover',
});

const props = defineProps<{
	nickname: string;
}>();

const displayName = computed(() => props.nickname.trim() || '游客');
</script>

<style scoped lang="scss">
.guest-user-popover__reference {
	display: inline-flex;
	align-items: center;
	cursor: pointer;

	&:focus-visible {
		outline: 2px solid var(--app-main);
		outline-offset: 2px;
	}
}

.guest-user-popover__content {
	display: flex;
	align-items: center;
	gap: 0.75rem;
}

.guest-user-popover__identity {
	min-width: 0;
	display: flex;
	flex-direction: column;

	strong,
	span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	span {
		color: var(--app-text-muted);
		font-size: 0.85rem;
	}
}
</style>
