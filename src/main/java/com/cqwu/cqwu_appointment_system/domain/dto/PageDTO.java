package com.cqwu.cqwu_appointment_system.domain.dto;

import java.util.List;
import lombok.Data;

/**
 * @description 通用的分页响应数据传输对象，用于将后端分页查询结果封装成前端友好的格式。
 * @param <T> 分页内容的类型
 */
@Data
public class PageDTO<T> {
    private List<T> content; // 当前页数据
    private int pageNumber; // 当前页码 (从 0 开始)
    private int pageSize; // 每页数量
    private long totalElements; // 总记录数
    private int totalPages; // 总页数
    private boolean last; // 是否为最后一页

    public PageDTO(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    /**
     * 静态工厂方法，用于从 Spring Data 的 Page 对象轻松创建 PageDTO。
     * 
     * @param page Spring Data Page 对象
     * @return 转换后的 PageDTO 对象
     */
    public static <T> PageDTO<T> fromPage(org.springframework.data.domain.Page<T> page) {
        return new PageDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}