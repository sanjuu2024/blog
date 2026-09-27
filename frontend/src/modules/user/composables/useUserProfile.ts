import { reactive, ref } from 'vue';
import { getCurrentUserProfile, getLikedArticles } from '../api/userApi';
import type { PublicArticleListItem } from '@/modules/article/types/article';
import { USER_ROLE, USER_STATUS, type CurrentUserInfo } from '../types/user';

export function useUserProfile() {
	// 用户信息默认值
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

	// 当前用户信息
	const userProfile = reactive<CurrentUserInfo>({ ...initUserProfile });
	const likedArticles = ref<PublicArticleListItem[]>([]);
	const likedArticlesLoading = ref(false);
	const likedArticlesLoadFailed = ref(false);
	const likedArticlesPage = ref(1);
	const likedArticlesTotalPages = ref(0);

	// 每页显示的文章数量
	const pageSize = ref(5);

	// 发送 获取个人信息 请求
	async function getUserProfile() {
		try {
			const data = await getCurrentUserProfile();
			Object.assign(userProfile, data);
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	async function getUserLikedArticles(pageNum: number = 1) {
		likedArticlesLoading.value = true;
		likedArticlesLoadFailed.value = false;
		try {
			const data = await getLikedArticles({ pageNum, pageSize: pageSize.value });
			likedArticles.value = data.records;
			likedArticlesPage.value = data.pageNum;
			likedArticlesTotalPages.value = data.totalPages;
		} catch {
			likedArticlesLoadFailed.value = true;
		} finally {
			likedArticlesLoading.value = false;
		}
	}

	return {
		pageSize,
		userProfile,
		getUserProfile,
		likedArticles,
		likedArticlesLoading,
		likedArticlesLoadFailed,
		likedArticlesPage,
		likedArticlesTotalPages,
		getUserLikedArticles,
	};
}
