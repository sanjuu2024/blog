<template>
	<section class="article-comment-section">
		<div class="comment-section-header">
			<h2 class="comment-section-title">评论</h2>
			<span class="comment-section-count">{{ article.commentCount }}</span>
		</div>

		<el-alert
			v-if="!article.allowComment"
			title="文章已关闭评论，已有评论仍可继续查看。"
			type="info"
			:closable="false"
			class="mb-4"
		/>

		<!-- 发表评论 -->
		<div class="comment-editor">
			<template v-if="article.allowComment && authStore.isLogin">
				<el-input
					v-model.trim="commentContent"
					type="textarea"
					:rows="4"
					maxlength="1000"
					show-word-limit
					placeholder="写下你的评论吧"
				/>
				<div class="mt-3 flex justify-end">
					<el-button
						type="primary"
						:loading="submitting"
						@click="submitComment"
					>
						发表评论
					</el-button>
				</div>
			</template>

			<div
				v-else-if="article.allowComment"
				class="comment-login-tip"
			>
				<span>登录后可以发表评论。</span>
				<el-button
					type="primary"
					link
					@click="goLogin"
				>
					去登录
				</el-button>
			</div>
		</div>

		<!-- 评论列表 -->
		<div
			v-if="commentList.length"
			class="comment-list"
		>
			<div
				v-for="comment in commentList"
				:key="comment.id"
				class="comment-item"
				data-comment-entry
			>
				<div class="comment-main">
					<PublicUserProfilePopover
						:user-id="comment.author.id"
						class="flex flex-col"
					>
						<AppUserAvatar
							:avatar-url="comment.author.avatarUrl"
							:name="comment.author.nickname || comment.author.username"
							:user-id="comment.author.id"
							:size="36"
							class="mr-3 shrink-0 self-start"
						/>
					</PublicUserProfilePopover>

					<div class="min-w-0 flex-1">
						<div class="comment-meta">
							<span class="comment-author">
								{{ comment.author.nickname || comment.author.username }}
							</span>
							<el-tag
								v-if="comment.status !== COMMENT_STATUS.APPROVED"
								:type="getCommentStatusTagType(comment.status)"
								size="small"
								class="ml-2"
							>
								{{ getCommentStatusLabel(comment.status) }}
							</el-tag>
							<span class="comment-time">{{
								formatDateTime(comment.createdAt)
							}}</span>
						</div>

						<PublicCommentContent
							:comment-id="comment.id"
							:content="comment.content"
							:show-actions="canReply(comment) || comment.isMine"
						>
							<template #reason>
								<p
									v-if="
										comment.status === COMMENT_STATUS.REJECTED &&
										comment.moderationReason
									"
									class="comment-reason"
								>
									拒绝原因：{{ comment.moderationReason }}
								</p>
							</template>
							<template #actions>
								<el-button
									v-if="canReply(comment)"
									link
									type="primary"
									@click="openReplyEditor(comment, comment.author)"
								>
									回复
								</el-button>

								<el-button
									v-if="comment.isMine"
									link
									type="danger"
									@click="clickDeleteTopLevelComment(comment)"
								>
									删除
								</el-button>
							</template>
						</PublicCommentContent>

						<!-- 回复编辑器 -->
						<PublicCommentReplyEditor
							v-if="isReplyEditorVisible(comment.id, comment.id)"
							v-model="replyContentMap[comment.id]"
							:target-name="replyTargetMap[comment.id]?.nickname || ''"
							:loading="submitting"
							@cancel="closeReplyEditor(comment.id)"
							@submit="submitCommentReply(comment)"
						/>

						<!-- 回复展开 / 折叠 -->
						<div
							v-if="comment.hasVisibleReplies"
							class="reply-toggle"
						>
							<el-button
								link
								type="primary"
								@click="clickToggleReplies(comment)"
							>
								<i-lucide-chevron-down
									class="mr-1 transition-transform duration-100 ease-in-out"
									:class="{ '-rotate-180': getReplyState(comment.id).expanded }"
								/>
								{{
									getReplyState(comment.id).expanded
										? '收起回复'
										: comment.replyCount > 0
											? `共 ${comment.replyCount} 条回复，点击查看`
											: '查看仅自己可见的回复'
								}}
							</el-button>
						</div>

						<!-- 评论的回复列表 -->
						<div
							v-if="getReplyState(comment.id).expanded"
							class="reply-list"
						>
							<div
								v-for="reply in getReplyState(comment.id).records"
								:key="reply.id"
								class="reply-item"
								data-comment-entry
							>
								<PublicUserProfilePopover
									:user-id="reply.author.id"
									class="flex flex-col"
								>
									<AppUserAvatar
										:avatar-url="reply.author.avatarUrl"
										:name="reply.author.nickname || reply.author.username"
										:user-id="reply.author.id"
										:size="28"
										class="mr-3 shrink-0 self-start"
									/>
								</PublicUserProfilePopover>

								<div class="min-w-0 flex-1">
									<div class="comment-meta">
										<span class="comment-author">
											{{ reply.author.nickname || reply.author.username }}
										</span>
										<span
											v-if="reply.replyToUser"
											class="reply-to-user"
										>
											回复 @{{
												reply.replyToUser.nickname ||
												reply.replyToUser.username
											}}
										</span>
										<el-tag
											v-if="reply.status !== COMMENT_STATUS.APPROVED"
											:type="getCommentStatusTagType(reply.status)"
											size="small"
											class="ml-2"
										>
											{{ getCommentStatusLabel(reply.status) }}
										</el-tag>
										<span class="comment-time">{{
											formatDateTime(reply.createdAt)
										}}</span>
									</div>

									<PublicCommentContent
										:comment-id="reply.id"
										:content="reply.content"
										:show-actions="canReply(reply) || reply.isMine"
									>
										<template #reason>
											<p
												v-if="
													reply.status === COMMENT_STATUS.REJECTED &&
													reply.moderationReason
												"
												class="comment-reason"
											>
												拒绝原因：{{ reply.moderationReason }}
											</p>
										</template>
										<template #actions>
											<el-button
												v-if="canReply(reply)"
												link
												type="primary"
												@click="
													openReplyEditor(comment, reply.author, reply.id)
												"
											>
												回复
											</el-button>

											<el-button
												v-if="reply.isMine"
												link
												type="danger"
												@click="clickDeleteReply(comment, reply)"
											>
												删除
											</el-button>
										</template>
									</PublicCommentContent>

									<PublicCommentReplyEditor
										v-if="isReplyEditorVisible(comment.id, reply.id)"
										v-model="replyContentMap[comment.id]"
										:target-name="replyTargetMap[comment.id]?.nickname || ''"
										:loading="submitting"
										@cancel="closeReplyEditor(comment.id)"
										@submit="submitCommentReply(comment)"
									/>
								</div>
							</div>

							<div class="reply-load-more">
								<el-button
									v-if="getReplyState(comment.id).hasNext"
									link
									type="primary"
									:loading="getReplyState(comment.id).loading"
									@click="getReplyList(comment.id)"
								>
									{{ getReplyLoadMoreLabel(comment) }}
								</el-button>
								<i-lucide-loader
									v-else-if="getReplyState(comment.id).loading"
									class="animate-spin text-gray-400"
								/>
							</div>
						</div>
					</div>
				</div>
			</div>

			<AppLoadMoreTrigger
				:loading="loading"
				:has-next="pageParams.hasNext"
				thing-str="评论"
				@load-more="loadMoreComments(article.id)"
			/>
		</div>

		<div
			v-else-if="!loading"
			class="comment-empty"
		>
			暂时还没有评论。
		</div>
	</section>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { formatDateTime } from '@/utils/datetime';
import AppLoadMoreTrigger from '@/components/AppLoadMoreTrigger.vue';
import AppUserAvatar from '@/components/AppUserAvatar.vue';
import { useAuthStore } from '@/stores/authStore';
import type { PublicArticleDetailData } from '@/modules/article/types/article';
import PublicCommentContent from './PublicCommentContent.vue';
import PublicCommentReplyEditor from './PublicCommentReplyEditor.vue';
import { usePublicCommentList } from '../composables/usePublicCommentList';
import {
	COMMENT_STATUS,
	type CommentAuthor,
	type CommentReplyItem,
	type CommentStatus,
	type PublicCommentItem,
} from '../types/comment';

defineOptions({
	name: 'PublicCommentSection',
});

const props = defineProps<{
	article: PublicArticleDetailData;
}>();

const emit = defineEmits<{
	commentCountChange: [delta: number];
}>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const {
	commentList,
	pageParams,
	loading,
	getReplyState,
	resetCommentList,
	loadMoreComments,
	getReplyList,
	collapseReplies,
	submitTopLevelComment,
	submitReply,
	deleteOwnComment,
	removeTopLevelComment,
	removeReply,
} = usePublicCommentList();

// 顶层评论输入框内容
const commentContent = ref('');

// 评论 / 回复提交中
const submitting = ref(false);

// 每条顶层评论下当前正在回复的目标用户
const replyTargetMap = reactive<Record<number, (CommentAuthor & { parentId: number }) | null>>({});

// 每条顶层评论下的回复输入框内容
const replyContentMap = reactive<Record<number, string>>({});

watch(
	() => props.article.id,
	(articleId) => {
		resetCommentList(articleId);
		commentContent.value = '';
	},
	{ immediate: true },
);

watch(
	() => authStore.accessToken,
	() => {
		resetCommentList(props.article.id);
	},
);

// 跳转到登录页
function goLogin() {
	router.push({
		path: '/auth/login',
		query: {
			redirect: route.fullPath,
		},
	});
}

// 判断是否可以回复评论
function canReply(comment: PublicCommentItem | CommentReplyItem) {
	return (
		props.article.allowComment &&
		authStore.isLogin &&
		comment.status === COMMENT_STATUS.APPROVED
	);
}

// 打开回复输入框
function openReplyEditor(rootComment: PublicCommentItem, user: CommentAuthor, parentId?: number) {
	if (!authStore.isLogin) {
		goLogin();
		return;
	}

	replyTargetMap[rootComment.id] = {
		...user,
		parentId: parentId ?? rootComment.id,
	};
	replyContentMap[rootComment.id] = '';
}

// 关闭回复输入框
function closeReplyEditor(rootCommentId: number) {
	replyTargetMap[rootCommentId] = null;
	replyContentMap[rootCommentId] = '';
}

// 判断回复输入框是否应该显示在当前评论 / 回复下方
function isReplyEditorVisible(rootCommentId: number, parentId: number) {
	return replyTargetMap[rootCommentId]?.parentId === parentId;
}

// 展开或收起回复列表
function clickToggleReplies(comment: PublicCommentItem) {
	const state = getReplyState(comment.id);

	if (state.expanded) {
		collapseReplies(comment.id);
	} else {
		getReplyList(comment.id, true);
	}
}

// 获取回复继续加载按钮文案
function getReplyLoadMoreLabel(comment: PublicCommentItem) {
	const remainingCount = Math.max(
		comment.replyCount -
			getReplyState(comment.id).records.filter(
				(reply) => reply.status === COMMENT_STATUS.APPROVED,
			).length,
		0,
	);

	return remainingCount > 0 ? `剩余 ${remainingCount} 条回复，点击展开` : '加载更多回复';
}

// 提交顶层评论
async function submitComment() {
	const content = commentContent.value.trim();
	if (!validateCommentContent(content)) return;

	submitting.value = true;

	try {
		const data = await submitTopLevelComment(props.article.id, content);
		commentContent.value = '';

		if (data.status === COMMENT_STATUS.APPROVED) {
			emit('commentCountChange', 1);
		}
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	} finally {
		submitting.value = false;
	}
}

// 提交回复
async function submitCommentReply(rootComment: PublicCommentItem) {
	const target = replyTargetMap[rootComment.id];
	const content = replyContentMap[rootComment.id]?.trim() || '';

	if (!target || !validateCommentContent(content)) return;

	submitting.value = true;

	try {
		const data = await submitReply(
			props.article.id,
			rootComment,
			target.parentId,
			target,
			content,
		);
		closeReplyEditor(rootComment.id);

		if (data.status === COMMENT_STATUS.APPROVED) {
			emit('commentCountChange', 1);
		}
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	} finally {
		submitting.value = false;
	}
}

// 点击删除顶层评论
async function clickDeleteTopLevelComment(comment: PublicCommentItem) {
	try {
		const data = await deleteOwnComment(comment);
		await removeTopLevelComment(props.article.id, comment);
		emit('commentCountChange', -data.deletedApprovedCount);
	} catch (error) {
		if (error === 'cancel' || error === 'close') return;
		// 错误提示已经由 request 响应拦截器统一处理
	}
}

// 点击删除回复
async function clickDeleteReply(rootComment: PublicCommentItem, reply: CommentReplyItem) {
	try {
		const data = await deleteOwnComment(reply);
		await removeReply(rootComment, reply, data.deletedApprovedCount);
		emit('commentCountChange', -data.deletedApprovedCount);
	} catch (error) {
		if (error === 'cancel' || error === 'close') return;
		// 错误提示已经由 request 响应拦截器统一处理
	}
}

// 校验评论内容
function validateCommentContent(content: string) {
	if (!content) {
		ElMessage.warning('评论内容不能为空');
		return false;
	}

	if (content.length > 1000) {
		ElMessage.warning('评论内容不能超过 1000 个字符');
		return false;
	}

	return true;
}

// 获取评论状态展示文案
function getCommentStatusLabel(status: CommentStatus) {
	switch (status) {
		case COMMENT_STATUS.PENDING:
			return '待审核';
		case COMMENT_STATUS.REJECTED:
			return '已拒绝';
		case COMMENT_STATUS.HIDDEN:
			return '已隐藏';
		case COMMENT_STATUS.DELETED:
			return '已删除';
		default:
			return '已通过';
	}
}

// 获取评论状态标签类型
function getCommentStatusTagType(status: CommentStatus) {
	switch (status) {
		case COMMENT_STATUS.PENDING:
			return 'warning';
		case COMMENT_STATUS.REJECTED:
		case COMMENT_STATUS.DELETED:
			return 'danger';
		default:
			return 'info';
	}
}
</script>

<style scoped lang="scss">
.article-comment-section {
	max-width: var(--app-article-detail-width);
	margin-inline: auto;
	padding: 2rem 0 4rem;
}

.comment-section-header {
	display: flex;
	align-items: center;
	margin-bottom: 1.25rem;
}

.comment-section-title {
	font-size: 1.5rem;
	font-weight: 700;
}

.comment-section-count {
	margin-left: 0.5rem;
	color: var(--app-text-muted);
}

.comment-editor {
	margin-bottom: 1.5rem;
}

.comment-login-tip {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 1rem;
	border-radius: 0.75rem;
	background-color: var(--app-surface-muted);
	color: var(--app-text-muted);
}

.comment-item {
	padding-block: 1.25rem;
	border-top: 1px solid var(--app-border);
	scroll-margin-top: calc(var(--app-header-height) + 1rem);
}

.comment-main,
.reply-item {
	display: flex;
}

.comment-meta {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 0.25rem;
}

.comment-author {
	font-weight: 600;
}

.comment-time,
.reply-to-user {
	margin-left: 0.5rem;
	color: var(--app-text-muted);
	font-size: 0.85rem;
}

.comment-reason {
	margin-top: 0.35rem;
	color: var(--el-color-danger);
	font-size: 0.9rem;
}

.reply-toggle {
	margin-top: 0.4rem;
}

.reply-list {
	margin-top: 0.75rem;
	padding: 0.75rem;
	border-radius: 0.75rem;
}

.reply-item {
	scroll-margin-top: calc(var(--app-header-height) + 1rem);
}

.reply-item + .reply-item {
	margin-top: 1rem;
}

.reply-load-more {
	display: flex;
	justify-content: center;
	margin-top: 0.75rem;
}

.comment-empty {
	padding: 2rem;
	text-align: center;
	color: var(--app-text-muted);
}
</style>
