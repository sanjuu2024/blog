import { nextTick } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createCategory, updateCategory } from '../api/adminCategoryApi';
import type { AdminCategoryItem } from '../types/adminCategory';
import { CATEGORY_LEVEL, CATEGORY_STATUS } from '@/modules/category/constants/category';
import { useAdminCategoryForm } from './useAdminCategoryForm';

const message = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }));

vi.mock('element-plus', () => ({
	ElMessage: message,
}));

vi.mock('../api/adminCategoryApi', () => ({
	createCategory: vi.fn(),
	updateCategory: vi.fn(),
}));

function category(): AdminCategoryItem {
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
	};
}

describe('useAdminCategoryForm', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(createCategory).mockResolvedValue({
			id: 20001,
			parentId: null,
			level: CATEGORY_LEVEL.FIRST,
			name: '技术',
			status: CATEGORY_STATUS.ENABLED,
			createdAt: '2026-09-01T10:00:00+08:00',
		});
		vi.mocked(updateCategory).mockResolvedValue({
			id: 21001,
			parentId: 20001,
			level: CATEGORY_LEVEL.SECOND,
			name: 'Java SE',
			status: CATEGORY_STATUS.ENABLED,
			updatedAt: '2026-09-01T11:00:00+08:00',
		});
	});

	it('opens create and update drawers with the expected form state', async () => {
		const form = useAdminCategoryForm();
		form.categoryForm.name = '旧值';

		form.openCreateDrawer();
		await nextTick();
		expect(form.drawerMode.value).toBe('create');
		expect(form.editingCategoryId.value).toBeNull();
		expect(form.categoryForm.name).toBe('');
		expect(form.showDrawer.value).toBe(true);

		form.openUpdateDrawer(category());
		await nextTick();
		expect(form.drawerMode.value).toBe('edit');
		expect(form.editingCategoryId.value).toBe(21001);
		expect(form.categoryForm).toMatchObject({
			parentId: 20001,
			level: CATEGORY_LEVEL.SECOND,
			name: 'Java',
		});
	});

	it('creates a trimmed first-level category without a parent', async () => {
		const form = useAdminCategoryForm();
		form.openCreateDrawer();
		await nextTick();
		Object.assign(form.categoryForm, {
			parentId: 99999,
			level: CATEGORY_LEVEL.FIRST,
			name: ' 技术 ',
			description: ' 技术文章 ',
		});
		form.setCategoryFormRef({ validate: vi.fn().mockResolvedValue(true) });

		await expect(form.handleUpsertCategory()).resolves.toBe(true);

		expect(createCategory).toHaveBeenCalledWith({
			parentId: null,
			level: CATEGORY_LEVEL.FIRST,
			name: '技术',
			description: '技术文章',
			sortNo: 100,
			status: CATEGORY_STATUS.ENABLED,
		});
		expect(form.showDrawer.value).toBe(false);
		expect(message.success).toHaveBeenCalledWith('分类 技术 创建成功');
	});

	it('updates the selected category', async () => {
		const form = useAdminCategoryForm();
		form.openUpdateDrawer(category());
		await nextTick();
		form.categoryForm.name = ' Java SE ';
		form.setCategoryFormRef({ validate: vi.fn().mockResolvedValue(true) });

		await expect(form.handleUpsertCategory()).resolves.toBe(true);

		expect(updateCategory).toHaveBeenCalledWith(
			21001,
			expect.objectContaining({ name: 'Java SE', parentId: 20001 }),
		);
		expect(message.success).toHaveBeenCalledWith('分类 Java SE 更新成功');
	});

	it('does not submit when form validation fails', async () => {
		const form = useAdminCategoryForm();
		form.setCategoryFormRef({ validate: vi.fn().mockRejectedValue(new Error('invalid')) });

		await expect(form.handleUpsertCategory()).resolves.toBe(false);

		expect(createCategory).not.toHaveBeenCalled();
		expect(updateCategory).not.toHaveBeenCalled();
		expect(message.error).toHaveBeenCalledWith('表单参数校验失败，请检查输入');
	});

	it('rejects update when the editing category id is missing', async () => {
		const form = useAdminCategoryForm();
		form.drawerMode.value = 'edit';
		form.setCategoryFormRef({ validate: vi.fn().mockResolvedValue(true) });

		await expect(form.handleUpsertCategory()).resolves.toBe(false);

		expect(updateCategory).not.toHaveBeenCalled();
		expect(message.error).toHaveBeenCalledWith('当前编辑的分类不存在，请关闭后重新打开编辑');
	});
});
