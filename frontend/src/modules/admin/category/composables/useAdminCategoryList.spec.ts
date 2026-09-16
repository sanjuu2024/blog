import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { deleteCategory, listCategories, updateCategory } from '../api/adminCategoryApi';
import type { AdminCategoryItem } from '../types/adminCategory';
import { CATEGORY_LEVEL, CATEGORY_STATUS } from '@/modules/category/constants/category';
import { useAdminCategoryList } from './useAdminCategoryList';

const message = vi.hoisted(() => ({ success: vi.fn() }));

vi.mock('element-plus', () => ({
	ElMessage: message,
}));

vi.mock('../api/adminCategoryApi', () => ({
	listCategories: vi.fn(),
	updateCategory: vi.fn(),
	deleteCategory: vi.fn(),
}));

function category(overrides: Partial<AdminCategoryItem> = {}): AdminCategoryItem {
	return {
		id: 21001,
		parentId: 20001,
		level: CATEGORY_LEVEL.SECOND,
		name: 'Java',
		description: 'Java 文章',
		sortNo: 10,
		status: CATEGORY_STATUS.ENABLED,
		articleCount: 8,
		createdAt: '2026-09-01T10:00:00+08:00',
		children: [],
		...overrides,
	};
}

describe('useAdminCategoryList', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(listCategories).mockResolvedValue([]);
		vi.mocked(deleteCategory).mockResolvedValue(null);
	});

	it('builds and sends second-level category filters', async () => {
		const list = useAdminCategoryList();
		Object.assign(list.queryParams, {
			keyword: 'Java',
			status: CATEGORY_STATUS.ENABLED,
			level: CATEGORY_LEVEL.SECOND,
			parentId: 20001,
		});
		vi.mocked(listCategories).mockResolvedValue([category()]);

		await list.getCategoryList();

		expect(listCategories).toHaveBeenCalledWith({
			keyword: 'Java',
			status: CATEGORY_STATUS.ENABLED,
			level: CATEGORY_LEVEL.SECOND,
			parentId: 20001,
		});
		expect(list.categoryList.value).toHaveLength(1);
	});

	it('does not send parentId for first-level or all-level queries', async () => {
		const list = useAdminCategoryList();
		list.queryParams.level = CATEGORY_LEVEL.FIRST;
		list.queryParams.parentId = 20001;

		await list.getCategoryList();

		expect(listCategories).toHaveBeenCalledWith(
			expect.objectContaining({ level: CATEGORY_LEVEL.FIRST, parentId: undefined }),
		);
	});

	it('updates the local status after toggling a category', async () => {
		const target = category();
		vi.mocked(updateCategory).mockResolvedValue({
			id: target.id,
			parentId: target.parentId,
			level: target.level,
			name: target.name,
			status: CATEGORY_STATUS.DISABLED,
			updatedAt: '2026-09-01T11:00:00+08:00',
		});
		const list = useAdminCategoryList();

		await list.toggleCategoryStatus(target);

		expect(updateCategory).toHaveBeenCalledWith(
			target.id,
			expect.objectContaining({ status: CATEGORY_STATUS.DISABLED }),
		);
		expect(target.status).toBe(CATEGORY_STATUS.DISABLED);
		expect(message.success).toHaveBeenCalledWith('分类 Java 已禁用');
	});

	it('refreshes the category list after deletion and resets filters', async () => {
		const list = useAdminCategoryList();
		list.queryParams.keyword = 'Java';
		list.queryParams.level = CATEGORY_LEVEL.SECOND;

		await expect(list.handleDeleteCategory(21001)).resolves.toBe(true);
		await flushPromises();
		expect(deleteCategory).toHaveBeenCalledWith(21001);
		expect(listCategories).toHaveBeenCalledTimes(1);

		list.resetQueryParams();
		await flushPromises();
		expect(list.queryParams).toMatchObject({
			keyword: undefined,
			status: '',
			level: 'ALL',
			parentId: undefined,
		});
		expect(listCategories).toHaveBeenCalledTimes(2);
	});
});
