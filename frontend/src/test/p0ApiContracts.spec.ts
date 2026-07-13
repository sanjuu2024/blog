import { beforeEach, describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { login, logout, refreshToken, register } from '@/modules/auth/api/authApi';
import {
	changeCurrentUserPassword,
	getCurrentUserProfile,
	updateCurrentUserProfile,
} from '@/modules/user/api/userApi';
import { listUsers, updateRole, updateStatus } from '@/modules/admin/user/api/adminUserApi';
import {
	createArticle,
	deleteArticle,
	listArticles,
	updateArticle,
	updateArticleStatus,
} from '@/modules/admin/article/api/adminArticleApi';
import {
	createCategory,
	deleteCategory,
	listCategories,
	updateCategory,
} from '@/modules/admin/category/api/adminCategoryApi';
import { createTag, deleteTag, listTags, updateTag } from '@/modules/admin/tag/api/adminTagApi';
import type { ArticleUpsertRequest } from '@/modules/admin/article/types/adminArticle';
import type { CategoryUpsertRequest } from '@/modules/admin/category/types/adminCategory';
import type { TagUpsertRequest } from '@/modules/admin/tag/types/adminTag';
import {
	getArticleDetails as getPublicArticleDetails,
	listArticles as listPublicArticles,
} from '@/modules/article/api/articleApi';
import { getEnabledCategories } from '@/modules/category/api/categoryApi';

vi.mock('@/utils/request', () => ({
	default: {
		get: vi.fn(),
		post: vi.fn(),
		put: vi.fn(),
		patch: vi.fn(),
		delete: vi.fn(),
	},
}));

describe('P0 frontend API contracts', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('uses the documented authentication endpoints', () => {
		const registerPayload = {
			username: 'sanjuu',
			email: 'sanjuu@example.com',
			password: 'Password_123',
		};
		const loginPayload = { account: 'sanjuu', password: 'Password_123' };

		register(registerPayload);
		login(loginPayload);
		refreshToken();
		logout();

		expect(request.post).toHaveBeenNthCalledWith(1, 'auth/register', registerPayload);
		expect(request.post).toHaveBeenNthCalledWith(2, 'auth/login', loginPayload);
		expect(request.post).toHaveBeenNthCalledWith(
			3,
			'auth/refresh',
			undefined,
			expect.objectContaining({
				meta: { skipAuthRefresh: true, showError: false },
			}),
		);
		expect(request.post).toHaveBeenNthCalledWith(4, 'auth/logout');
	});

	it('uses the documented personal-center endpoints', () => {
		const profilePayload = { nickname: 'Sanjuu', bio: 'Hello' };
		const passwordPayload = { oldPassword: 'Old_123', newPassword: 'New_123' };

		getCurrentUserProfile();
		updateCurrentUserProfile(profilePayload);
		changeCurrentUserPassword(passwordPayload);

		expect(request.get).toHaveBeenCalledWith('users/me', undefined);
		expect(request.put).toHaveBeenNthCalledWith(1, 'users/me/profile', profilePayload);
		expect(request.put).toHaveBeenNthCalledWith(2, 'users/me/password', passwordPayload);
	});

	it('uses public article and category browsing endpoints', () => {
		const query = {
			pageNum: 1,
			pageSize: 10,
			categoryId: 21001,
			tagIds: [30001],
		};

		listPublicArticles(query);
		getPublicArticleDetails(40001);
		getEnabledCategories();

		expect(request.get).toHaveBeenNthCalledWith(1, 'articles', {
			params: query,
		});
		expect(request.get).toHaveBeenNthCalledWith(2, 'articles/40001');
		expect(request.get).toHaveBeenNthCalledWith(3, 'categories', { params: null });
	});

	it('sends user-management queries and mutations to admin endpoints', () => {
		const query = {
			pageNum: 2,
			pageSize: 10,
			role: 'USER' as const,
			status: 'ACTIVE' as const,
		};

		listUsers(query);
		updateStatus(10002, { status: 'DISABLED' });
		updateRole(10002, { role: 'ADMIN' });

		expect(request.get).toHaveBeenCalledWith('admin/users', {
			params: query,
			meta: { showError: false },
		});
		expect(request.patch).toHaveBeenNthCalledWith(1, 'admin/users/10002/status', {
			status: 'DISABLED',
		});
		expect(request.patch).toHaveBeenNthCalledWith(2, 'admin/users/10002/role', {
			role: 'ADMIN',
		});
	});

	it('supports article listing, draft creation, publishing, offline and deletion', () => {
		const draft: ArticleUpsertRequest = {
			title: 'Article',
			contentMd: '# Article',
			categoryId: 21001,
			tagIds: [30001],
			status: 'DRAFT',
		};

		listArticles({ pageNum: 1, pageSize: 10, status: 'DRAFT' });
		createArticle(draft);
		updateArticle(40001, { ...draft, status: 'PUBLISHED' });
		updateArticleStatus(40001, { status: 'OFFLINE' });
		deleteArticle(40001);

		expect(request.get).toHaveBeenCalledWith('admin/articles', {
			params: { pageNum: 1, pageSize: 10, status: 'DRAFT' },
		});
		expect(request.post).toHaveBeenCalledWith('admin/articles', draft);
		expect(request.put).toHaveBeenCalledWith(
			'admin/articles/40001',
			expect.objectContaining({ status: 'PUBLISHED' }),
		);
		expect(request.patch).toHaveBeenCalledWith('admin/articles/40001/status', {
			status: 'OFFLINE',
		});
		expect(request.delete).toHaveBeenCalledWith('admin/articles/40001');
	});

	it('supports category management endpoints', () => {
		const category: CategoryUpsertRequest = {
			parentId: 20001,
			level: 2,
			name: 'Java',
			status: 'ENABLED',
		};

		listCategories({ level: 2, status: 'ENABLED' });
		createCategory(category);
		updateCategory(21001, category);
		deleteCategory(21001);

		expect(request.get).toHaveBeenCalledWith('admin/categories', {
			params: { level: 2, status: 'ENABLED' },
		});
		expect(request.post).toHaveBeenCalledWith('admin/categories', category);
		expect(request.put).toHaveBeenCalledWith('admin/categories/21001', category);
		expect(request.delete).toHaveBeenCalledWith('admin/categories/21001');
	});

	it('supports tag management endpoints', () => {
		const tag: TagUpsertRequest = {
			name: 'Spring Boot',
			description: 'Spring Boot articles',
			status: 'ENABLED',
		};

		listTags({ status: 'ENABLED' });
		createTag(tag);
		updateTag(30001, tag);
		deleteTag(30001);

		expect(request.get).toHaveBeenCalledWith('admin/tags', {
			params: { status: 'ENABLED' },
		});
		expect(request.post).toHaveBeenCalledWith('admin/tags', tag);
		expect(request.put).toHaveBeenCalledWith('admin/tags/30001', tag);
		expect(request.delete).toHaveBeenCalledWith('admin/tags/30001');
	});
});
