package com.cqwu.cqwu_appointment_system.common.converter;

import java.util.List;

@SuppressWarnings("unused")
public class PageDTO<T> {
    private List<T> content;      // 当前页数据
    private int pageNumber;     // 当前页码 (从 0 开始)
    private int pageSize;       // 每页数量
    private long totalElements;  // 总记录数
    private int totalPages;     // 总页数
    private boolean last;         // 是否为最后一页

    // 构造函数
    public PageDTO(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    // 可以提供一个静态工厂方法来简化从 Page 对象的转换
    public static <T> PageDTO<T> fromPage(org.springframework.data.domain.Page<T> page) {
        return new PageDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}