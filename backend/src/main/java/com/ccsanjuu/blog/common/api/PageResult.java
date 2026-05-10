package com.ccsanjuu.blog.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T>{
    private long total;   // 总记录数
    private long totalPages;   // 总页码数
    private long pageNum;   // 当前页码
    private long pageSize;   // 每页记录数
    private boolean hasNext;   // 当前页后面是否有下一页
    private List<T> records;   // 当前页的数据列表

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }

        long safeTotal = Math.max(total, 0);
        long safePageNum = Math.max(pageNum, 1);
        List<T> safeRecords = records == null ? List.of() : records;
        long totalPages = Math.ceilDiv(safeTotal, pageSize);

        return PageResult.<T>builder()
                .total(safeTotal)
                .totalPages(totalPages)
                .pageNum(safePageNum)
                .pageSize(pageSize)
                .hasNext(safePageNum < totalPages)
                .records(safeRecords)
                .build();
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
