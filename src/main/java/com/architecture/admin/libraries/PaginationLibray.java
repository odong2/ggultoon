package com.architecture.admin.libraries;

import com.architecture.admin.models.dto.SearchDto;
import lombok.Data;

@Data
public class PaginationLibray {
    private int totalRecordCount;   // 전체 데이터 수
    private int totalPageCount;     // 전체 페이지 수
    private int startPage;          // 첫 페이지 번호
    private int endPage;            // 끝 페이지 번호
    private int limitStart;         // LIMIT 시작 위치
    private boolean existPrevPage;  // 이전 페이지 존재 여부
    private boolean existNextPage;  // 다음 페이지 존재 여부

    public PaginationLibray(int totalRecordCount, SearchDto searchDto) {
        if (totalRecordCount > 0) {
            this.totalRecordCount = totalRecordCount;
            this.calculation(searchDto);
        }
    }

    private void calculation(SearchDto searchDto) {

        // 전체 페이지 수 계산
        totalPageCount = ((totalRecordCount - 1) / searchDto.getRecordSize()) + 1;

        // 첫 페이지 번호 계산
        startPage = ((searchDto.getPage() - 1) / searchDto.getPageSize()) * searchDto.getPageSize() + 1;

        // 끝 페이지 번호 계산
        endPage = startPage + searchDto.getPageSize() - 1;

        // 끝 페이지가 전체 페이지 수보다 큰 경우, 끝 페이지 전체 페이지 수 저장
        if (endPage > totalPageCount) {
            endPage = totalPageCount;
        }

        // LIMIT 시작 위치 계산
        limitStart = (searchDto.getPage() - 1) * searchDto.getRecordSize();

        // 이전 페이지 존재 여부 확인
        existPrevPage = startPage != 1;

        // 다음 페이지 존재 여부 확인
        existNextPage = (endPage * searchDto.getRecordSize()) < totalRecordCount;
    }


}

