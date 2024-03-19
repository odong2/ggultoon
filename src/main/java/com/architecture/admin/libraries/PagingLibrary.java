package com.architecture.admin.libraries;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*****************************************************
 * 페이징 라이브러리
 ****************************************************/
@Component
public class PagingLibrary {
    /**
     * Attribute 선언
     */
    private Boolean bView = false;
    private Boolean bFirst = false;
    private Boolean bLast = false;
    private Boolean bPrev = false;
    private Boolean bNext = false;
    private Integer iPage = 1;
    private Integer iTotalCount = 0;
    private Integer iRecords = 20;
    private Integer iBlocks = 5;
    private String sType = "link";
    private String sFuncName = "";
    private Map<String, Object> hmParams;

    /**
     * 빌드 함수
     */
    public Map<String, Object> buildPaging() {
        int iTotalPage = (int)Math.ceil(iTotalCount / (double)iRecords);
        int iTotalBlock = (int)Math.ceil(iTotalPage / (double)iBlocks);
        int iNowBlock = (int)Math.ceil(iPage / (double)iBlocks);
        int iStartPage = (iNowBlock - 1) * iBlocks + 1;
        int iEndPage = iStartPage + iBlocks - 1;

        if ( iEndPage > iTotalPage ) {
            iEndPage = iTotalPage;
        }
        if ( iPage != 1 ) {
            bFirst = true;
        }
        if ( iNowBlock < iTotalBlock ) {
            bNext = true;
        }
        if ( iNowBlock > 1 ) {
            bPrev = true;
        }
        if ( iPage < iTotalPage ) {
            bLast = true;
        }

        ArrayList<Integer> aPages = new ArrayList<>();
        if ( iEndPage > 1 ) {
            for ( int i = iStartPage; i <= iEndPage; i++ ) {
                aPages.add(i);
            }
            bView = true;
        }

        if ( !sFuncName.isEmpty() ) {
            sType = "func";
        }

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        hmParams.forEach((key, value) -> {
            if ( !key.equals("page") ) {
                builder.queryParam(key, value).encode(StandardCharsets.UTF_8);
            }
        });
        UriComponents result = builder.build();

        HashMap<String, Object> hmPaging = new HashMap<>();
        hmPaging.put("isView", bView);
        hmPaging.put("isFirst", bFirst);
        hmPaging.put("isLast", bLast);
        hmPaging.put("isPrev", bPrev);
        hmPaging.put("isNext", bNext);
        hmPaging.put("page", iPage);
        hmPaging.put("firstPage", 1);
        hmPaging.put("prevPage", iStartPage - 1);
        hmPaging.put("nextPage", iStartPage + iBlocks);
        hmPaging.put("nowPage", iPage);
        hmPaging.put("lastPage", iTotalPage);
        hmPaging.put("pages", aPages);
        hmPaging.put("totalCount", iTotalCount);
        hmPaging.put("records", iRecords);
        hmPaging.put("blocks", iBlocks);
        hmPaging.put("params", hmParams);
        hmPaging.put("link", "?" + result.getQuery());
        hmPaging.put("type", sType);
        hmPaging.put("functionName", sFuncName);

        return hmPaging;
    }

    /**
     * Set 함수
     */
    // Set page
    public PagingLibrary page(Integer iPage) {
        this.iPage = iPage;
        return this;
    }
    // Set Total Count
    public PagingLibrary totalCount(Integer iTotalCount) {
        this.iTotalCount = iTotalCount;
        return this;
    }
    // Set Records
    public PagingLibrary records(Integer iRecords) {
        this.iRecords = iRecords;
        return this;
    }
    // Set Records
    public PagingLibrary blocks(Integer iBlocks) {
        this.iBlocks = iBlocks;
        return this;
    }
    // Set Params
    public PagingLibrary params(Map<String, Object> hmParams) {
        this.hmParams = hmParams;
        return this;
    }
    // Set JsFunctionName
    public PagingLibrary funcName(String sFuncName) {
        this.sFuncName = sFuncName;
        return this;
    }
}
