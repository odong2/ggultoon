package com.architecture.admin.services.community;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.S3Library;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.community.CommunityDao;
import com.architecture.admin.models.daosub.community.CommunityDaoSub;
import com.architecture.admin.models.daosub.content.ContentDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.community.CommunityContentDto;
import com.architecture.admin.models.dto.community.CommunityImageDto;
import com.architecture.admin.models.dto.content.ContentDto;
import com.architecture.admin.models.dto.content.ContentImgDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.architecture.admin.config.SessionConfig.LOGIN_NICK;

@RequiredArgsConstructor
@Service
public class CommunityService extends BaseService {

    private static final String CONTENT_LIKE = "like";               // 게시물 좋아요
    private static final String CONTENT_LIKE_CANCEL = "likeCancel";  // 게시물 좋아요 취소
    private static final int CONTENT_MAX_SIZE = 500;                 // 컨텐츠 최대 크기
    private static final int CONTENT_NORMAL = 1;                     // 일반 게시물
    private static final int CONTENT_PROMOTION = 2;                  // 작품 홍보 게시물
    private static final int UPLOAD_MAX_SIZE = 10485760;             // 10MB
    private final CommunityDao communityDao;
    private final CommunityDaoSub communityDaoSub;
    private final ContentDaoSub contentDaoSub;
    private final S3Library s3Library;

    /**************************************************************************************
     * Select
     **************************************************************************************/

    /**
     * 커뮤니티 게시물 리스트
     *
     * @param searchDto
     * @return
     */
    public JSONObject getContentList(SearchDto searchDto) {
        /** 게시물 리스트 유효성 검사 **/
        contentListValidate(searchDto);

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        // 로그인 상태
        if (memberInfo != null) {
            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx);                  // Long 형변환
            searchDto.setMemberIdx(memberIdx);                               // 회원 idx set

            /** 신고한 목록 조회 **/
            List<Long> reportIdxList = communityDaoSub.getContentsReportList(memberIdx);
            // 신고 리스트 set -> 조회된 항목 없는경우 null 값
            searchDto.setIdxList(reportIdxList);
            /** 성인 여부 **/
            Integer adult = Integer.valueOf(super.getMemberInfo(SessionConfig.ADULT));
            searchDto.setAdult(adult);
        }

        // 게시물 개수 조회
        int totalCnt = communityDaoSub.getContentsTotalCnt(searchDto);

        // paging
        PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
        searchDto.setPagination(pagination);

        //return value
        JSONObject jsonData = new JSONObject();
        List<CommunityContentDto> contentList = null;

        if (totalCnt > 0) {
            // 게시물 리스트 조회
            contentList = communityDaoSub.getContentList(searchDto);
            searchDto.setIdxList(null);               // 앞단에서 쓰지 않으므로 null 처리
            searchDto.setMemberIdx(null);

            setContentTime(contentList);              // 등록일 시간 set
            setCommunityListImgFullUrl(contentList);  // 이미지 url set
            setLikeBoolean(contentList);              // 좋아요 set
            jsonData.put("params", new JSONObject(searchDto)); // 페이징 set
        }

        jsonData.put("contentList", contentList);

        return jsonData;
    }

    /**
     * 커뮤니티 게시물 상세
     *
     * @param idx
     * @return
     */
    public JSONObject getContentInfo(Long idx) {

        /** 커뮤니티 게시물 상세 유효성 검사 **/
        contentInfoValidate(idx);

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);
        SearchDto searchDto = new SearchDto();
        searchDto.setIdx(idx);
        CommunityContentDto communityContent;

        // 로그인 상태
        if (memberInfo != null) {
            String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
            Long memberIdx = Long.valueOf(stringMemberIdx);                  // Long 형변환
            searchDto.setMemberIdx(memberIdx);                               // 회원 idx set
            // 게시물 상세 조회
            communityContent = communityDaoSub.getContentInfo(searchDto);
            // 게시물 좋아요 상태
            if (communityContent.getMemberLike() > 0) {
                communityContent.setIsMemberLike(true);
                // 게시물 좋아요 상태 아님
            } else {
                communityContent.setIsMemberLike(false);
            }
            communityContent.setMemberLike(null);
            // 비로그인 상태
        } else {
            // 게시물 상세 조회
            communityContent = communityDaoSub.getContentInfo(searchDto);
            communityContent.setIsMemberLike(false);
        }

        setCommunityImgFulUrl(communityContent.getImageList()); // 이미지 도메인 set
        setContentTime(communityContent); // 등록 시간 set

        ContentDto content = new ContentDto();

        // 컨텐츠가 있는 경우(작품 홍보인 경우)
        if (communityContent.getContentsIdx() != null && communityContent.getContentsIdx() > 0) {
            ContentDto contentDto = ContentDto.builder()
                    .idx(communityContent.getContentsIdx())
                    .nowDate(dateLibrary.getDatetime()).build();
            // 컨텐츠 조회
            content = communityDaoSub.getContent(contentDto);
            setContentImgFulUrl(content.getContentHeightImgList()); // 이미지 도메인 set
        }

        //return value
        JSONObject jsonData = new JSONObject();

        jsonData.put("communityContent", new JSONObject(communityContent));
        jsonData.put("content", new JSONObject(content));

        return jsonData;
    }

    /**
     * 커뮤니티 게시물 상세 유효성 검사
     *
     * @param idx
     */
    private void contentInfoValidate(Long idx) {
        /** 커뮤니티 게시물 idx 유효성 검사 **/
        idxValidate(idx);
        // 게시물 성인 여부 조회
        int adult = communityDaoSub.getContentsAdult(idx);

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        // 로그인 하지 않고 성인 게시물
        if (memberInfo == null) {
            if (adult == 1) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
            // 로그인 상태
        } else {
            /** 성인 여부 **/
            Integer memberAdult = Integer.valueOf(super.getMemberInfo(SessionConfig.ADULT));
            // 성인 게시물이고, 회원 비성인
            if (adult == 1 && memberAdult == 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT);  // 성인인증 후 이용이 가능합니다.
            }
        }
    }


    /**************************************************************************************
     * Insert
     **************************************************************************************/

    /**
     * 커뮤니티 게시물 등록
     *
     * @param contentDto : memberIdx(회원 idx)
     */
    @Transactional
    public void registerContent(CommunityContentDto contentDto) {

        /** 커뮤니티 게시물 등록 유효성 검사 **/
        registerContentValidate(contentDto);

        /** 컨텐츠 테이블 insert **/
        contentDto.setRegdate(super.dateLibrary.getDatetime());

        int result = communityDao.insertContents(contentDto);

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_REGISTER_ERROR); // 게시물 등록에 실패하였습니다.
        }
        /** 컨텐츠 info 테이블 insert **/

        result = communityDao.insertContentsInfo(contentDto);

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_REGISTER_ERROR); // 게시물 등록에 실패하였습니다.
        }

        // s3에 저장될 path
        String s3Path = "community/" + contentDto.getInsertedIdx();

        List<MultipartFile> uploadFiles = contentDto.getUploadFiles();

        // 파일 있는지 체크
        Boolean chkHeightImage = chkIsEmptyImage(uploadFiles);

        if (Boolean.TRUE.equals(chkHeightImage)) {
            /** 이미지 유효성 검사 **/
            imageValidation(uploadFiles); // contentType 및 용량 체크

            // s3 upload(원본)
            List<HashMap<String, Object>> uploadResponse = s3Library.uploadFileNew(uploadFiles, s3Path);

            // 리사이징 이미지 구하기
            uploadResponse = imageSize(uploadResponse, 328);

            // db insert
            registerImage(uploadResponse, contentDto.getInsertedIdx(), s3Path);
        }

    }

    /**
     * 이미지 등록
     *
     * @param uploadResponse
     * @param idx
     * @param s3Path
     */
    public void registerImage(List<HashMap<String, Object>> uploadResponse, Long idx, String s3Path) {

        for (HashMap<String, Object> map : uploadResponse) {
            map.put("idx", idx);
            map.put("parent", 0);
            map.put("path", s3Path);
            map.put("sort", 1);
            map.put("device", "origin");
            map.put("regdate", dateLibrary.getDatetime());
        }
        // 이미지 등록
        int result = communityDao.registerImage(uploadResponse);

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_REGISTER_ERROR); // 게시물 등록을 실패하였습니다.
        }
    }

    /**
     * 커뮤니티 게시물 신고하기
     *
     * @param contentDto : idx(커뮤니티 게시물 idx)
     */
    public void reportContent(CommunityContentDto contentDto) {

        /** 커뮤니티 게시물 IDX 유효성 검사 **/
        idxValidate(contentDto.getIdx());

        // 게시물 신고 한적 있는지 조회
        int reportCnt = communityDaoSub.getReportCnt(contentDto);

        if (reportCnt == 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENT_REPORT_EXIST); // 이미 신고한 게시물입니다.
        }

        // 게시물 신고하기
        contentDto.setRegdate(dateLibrary.getDatetime()); // 현재 시간 set

        try {
            int result = communityDao.insertContentsReport(contentDto);

            if (result < 1) {
                throw new CustomException(CustomError.COMMUNITY_CONTENT_REPORT_FAIL); // 게시물 신고를 실패하였습니다.
            }
        } catch (DuplicateKeyException e) {
            throw new CustomException(CustomError.COMMUNITY_CONTENT_REPORT_EXIST); // 이미 신고한 게시물입니다.
        }
    }


    /**************************************************************************************
     * Update
     **************************************************************************************/

    /**
     * 게시물 좋아요
     *
     * @param contentDto : idx(게시물 idx), memberIdx(회원 idx)
     * @return
     */
    public String updateContentLike(CommunityContentDto contentDto) {

        /** 커뮤니티 게시물 IDX 유효성 검사 **/
        idxValidate(contentDto.getIdx());

        // 1. 좋아요 한적 있는지 조회 (state 조회)
        Integer state = communityDaoSub.getMemContentLikeState(contentDto); // (0: 좋아요 취소상태, 1: 좋아요 상태, null : 좋아요 한적 없음)
        String likeType = CONTENT_LIKE;

        contentDto.setRegdate(dateLibrary.getDatetime()); // 날짜 set
        int result = 0; // 업데이트 결과

        // 1. 좋아요 한적 없음
        if (state == null) {
            contentDto.setState(1);                                   // 정상 set
            result = communityDao.insertMemContentsLike(contentDto);  // 좋아요 테이블 insert

            // 2. 좋아요 한적있음
        } else {
            // 현재 좋아요 상태 -> 좋아요 취소
            if (state == 1) {
                likeType = CONTENT_LIKE_CANCEL;
                contentDto.setState(0); // 삭제 set
                result = communityDao.updateMemContentsLike(contentDto); // 좋아요 취소

                // 현재 좋아요 상태 아님 -> 좋아요
            } else {
                contentDto.setState(1);
                result = communityDao.updateMemContentsLike(contentDto); // 좋아요
            }
        }

        // insert 또는 update 실패 시
        if (result < 1) {
            if (likeType.equals(CONTENT_LIKE)) { // 좋아요
                throw new CustomException(CustomError.COMMUNITY_CONTENTS_LIKE_FAIL);        // 게시물 좋아요를 실패하였습니다.
            } else { // 좋아요 취소
                throw new CustomException(CustomError.COMMUNITY_CONTENTS_LIKE_CANCEL_FAIL); // 게시물 좋아요 취소를 실패하였습니다.
            }
        }

        /** contents_info 테이블 좋아요수 업데이트 **/
        contentsInfoLikeCntUpdate(contentDto, likeType);

        return likeType; // 좋아요 or 좋아요 취소
    }

    /**
     * 댓글 테이블 좋아요 수 업데이트
     *
     * @param contentDto : idx(게시물 idx), memberId(회원 idx)
     * @param likeType   : like(좋아요), likeCancel(좋아요 취소)
     */
    private void contentsInfoLikeCntUpdate(CommunityContentDto contentDto, String likeType) {

        /** 1. 현재 게시물 좋아요 개수 가지고 오기 **/
        CommunityContentDto contentInfoDto = communityDao.getContentsInfoByIdx(contentDto.getIdx());

        int commentLikeCnt = contentInfoDto.getLikeCnt(); // 게시물 좋아요 개수

        // 좋아요 취소
        if (likeType.equals(CONTENT_LIKE_CANCEL)) {
            commentLikeCnt = commentLikeCnt - 1; // 좋아요 수 감소

            // 좋아요
        } else if (likeType.equals(CONTENT_LIKE)) {
            commentLikeCnt = commentLikeCnt + 1; // 좋아요 수 증가
        }

        // 업데이트 할 좋아요 수 set
        contentDto.setLikeCnt(commentLikeCnt);

        /** 2. contents_info 테이블 좋아요수 업데이트 **/
        int updateResult = communityDao.updateContentsLikeCnt(contentDto);

        /** 3. 업데이트 결과에 따른 rollback **/
        if (updateResult < 1 && likeType.equals(CONTENT_LIKE)) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_LIKE_FAIL); // 댓글 좋아요를 실패하였습니다.

        } else if (updateResult < 1 && likeType.equals(CONTENT_LIKE_CANCEL)) {
            throw new CustomException(CustomError.COMMUNITY_COMMENT_LIKE_CANCEL_FAIL);// 댓글 좋아요 취소를 실패하였습니다.
        }
    }


    /**************************************************************************************
     * Delete
     **************************************************************************************/

    /**
     * 커뮤니티 게시물 삭제
     *
     * @param contentDto : idx(게시물 idx), memberIdx(회원 idx)
     */
    @Transactional
    public void deleteContent(CommunityContentDto contentDto) {
        /** 삭제 유효성 검사 **/
        deleteContentValidate(contentDto);
        // 1. 컨텐츠 삭제
        int result = communityDao.deleteContent(contentDto.getIdx());

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_DELETE_FAIL); // 게시물 삭제를 실패하였습니다.
        }
        // 2. info 테이블 삭제
        result = communityDao.deleteContentInfo(contentDto.getIdx());

        if (result < 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_DELETE_FAIL); // 게시물 삭제를 실패하였습니다.
        }

        // 3. 이미지 조회
        CommunityImageDto imageDto  = communityDaoSub.getContentImage(contentDto.getIdx());

        if (imageDto != null) {

            result = communityDao.deleteContentImage(contentDto.getIdx());

            if (result < 1) {
                throw new CustomException(CustomError.COMMUNITY_CONTENTS_DELETE_FAIL); // 게시물 삭제를 실패하였습니다.
            }
            // 이미지 삭제
            s3Library.deleteFile(imageDto.getUrl());

        }


    }


    /**************************************************************************************
     * Sub
     **************************************************************************************/

    /**
     * 좋아요 여부 불리언 변환
     *
     * @param contentList
     */
    private void setLikeBoolean(List<CommunityContentDto> contentList) {
        for (CommunityContentDto contentDto : contentList) {
            if (contentDto != null) {
                // 좋아요 안한 상태
                if (contentDto.getMemberLike() == null || contentDto.getMemberLike() == 0) {
                    contentDto.setIsMemberLike(false); // 좋아요 안함
                    contentDto.setMemberLike(null);    // 변환 후 null 처리
                    // 좋아요 상태
                } else if (contentDto.getMemberLike() == 1) {
                    contentDto.setIsMemberLike(true);  // 좋아요 함
                    contentDto.setMemberLike(null);    // 변환 후 null 처리
                }
            }
        }
    }

    /**
     * 컨텐츠 이미지 url setting
     *
     * @param contentList
     * @return
     */
    public void setCommunityListImgFullUrl(List<CommunityContentDto> contentList) {

        for (CommunityContentDto contentDto : contentList) {

            // 컨텐츠 세로 이미지 리스트 url setting
            setCommunityImgFulUrl(contentDto.getImageList());
        }
    }

    /**
     * 컨텐츠 이미지 fulUrl 세팅
     * 리사이징 된 이미지(s3Library.getThumborFullUrl())
     *
     * @param contentImgDtoList
     */
    private void setCommunityImgFulUrl(List<CommunityImageDto> contentImgDtoList) {

        if (contentImgDtoList != null && !contentImgDtoList.isEmpty()) {

            for (CommunityImageDto contentImgDto : contentImgDtoList) {

                if (contentImgDto.getUrl() != null) {

                    Map<String, Object> map = new HashMap<>();
                    map.put("fileUrl", contentImgDto.getUrl());     // 이미지 url
                    map.put("width", contentImgDto.getWidth());     // 이미지 가로 사이즈
                    map.put("height", contentImgDto.getHeight());   // 이미지 세로 사이즈

                    String fullUrl = s3Library.getThumborFullUrl(map);
                    contentImgDto.setUrl(fullUrl);
                }
            }
        }
    }

    /**
     * 컨텐츠 이미지 url setting
     *
     * @param contentList
     * @return
     */
    public void setContentListImgFullUrl(List<ContentDto> contentList) {

        for (ContentDto contentDto : contentList) {

            // 컨텐츠 세로 이미지 리스트 url setting
            setContentImgFulUrl(contentDto.getContentHeightImgList());
        }
    }

    /**
     * 컨텐츠 이미지 fulUrl 세팅
     * 리사이징 된 이미지(s3Library.getThumborFullUrl())
     *
     * @param contentImgDtoList
     */
    private void setContentImgFulUrl(List<ContentImgDto> contentImgDtoList) {

        if (contentImgDtoList != null && !contentImgDtoList.isEmpty()) {

            for (ContentImgDto contentImgDto : contentImgDtoList) {

                if (contentImgDto.getUrl() != null) {

                    Map<String, Object> map = new HashMap<>();
                    map.put("fileUrl", contentImgDto.getUrl());     // 이미지 url
                    map.put("width", contentImgDto.getWidth());     // 이미지 가로 사이즈
                    map.put("height", contentImgDto.getHeight());   // 이미지 세로 사이즈

                    String fullUrl = s3Library.getThumborFullUrl(map);
                    contentImgDto.setUrl(fullUrl);
                }
            }
        }
    }

    /**
     * 게시물 등록일 지난 시간 변환
     * ex. 3분 전 / 1시간 전 / 4월 26일 등
     *
     * @param contentDtoList
     */
    private void setContentTime(List<CommunityContentDto> contentDtoList) {

        for (CommunityContentDto contentDto : contentDtoList) {

            if (contentDto != null) {

                // 댓글 등록일 지난 시간 변환
                String regdate = dateLibrary.getConvertRegdate(contentDto.getRegdate());
                contentDto.setRegdate(regdate);
            }
        }
    }

    /**
     * 게시물 등록일 지난 시간 변환
     * ex. 3분 전 / 1시간 전 / 4월 26일 등
     *
     * @param contentDto
     */
    private void setContentTime(CommunityContentDto contentDto) {

        if (contentDto != null) {

            // 댓글 등록일 지난 시간 변환
            String regdate = dateLibrary.getConvertRegdate(contentDto.getRegdate());
            contentDto.setRegdate(regdate);
        }
    }

    /**
     * 리사이징 이미지 사이즈 구하기
     *
     * @param uploadResponse
     * @param width
     * @return
     */
    public List<HashMap<String, Object>> imageSize(List<HashMap<String, Object>> uploadResponse, int width) {
        for (HashMap<String, Object> data : uploadResponse) {
            String url = data.get("fileUrl").toString();
            String fullUrl = s3Library.getUploadedFullUrl(url);
            String suffix = url.substring(url.lastIndexOf('.') + 1).toLowerCase();

            Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);

            if (iter.hasNext()) {
                ImageReader reader = iter.next();
                try {
                    InputStream is = new URL(fullUrl).openStream();
                    ImageInputStream stream = ImageIO.createImageInputStream(is);

                    reader.setInput(stream);
                    int widthOrg = reader.getWidth(reader.getMinIndex());
                    int heightOrg = reader.getHeight(reader.getMinIndex());

                    int height = (width * heightOrg / widthOrg);

                    data.put("width", width);
                    data.put("height", height);
                } catch (IOException e) {
                } finally {
                    reader.dispose();
                }
            }
        }

        return uploadResponse;
    }

    /*
     * List<MultipartFile> image empty 체크
     * List 형식임으로 각 image 별로 isEmpty() 체크 처리
     * return Boolean
     */
    public Boolean chkIsEmptyImage(List<MultipartFile> uploadFileImgList) {

        boolean isEmptyValue = false;

        for (MultipartFile image : uploadFileImgList) {
            // 이미지 존재
            if (!image.isEmpty()) {
                isEmptyValue = true;
            }
        }
        return isEmptyValue;
    }

    /**
     * 커뮤니티 게시물 이미지 유효성 체크
     *
     * @param uploadFiles
     */
    private void imageValidation(List<MultipartFile> uploadFiles) {

        for (MultipartFile multipartFile : uploadFiles) {
            /** 컨텐트 타입 체크 **/
            String contentType = multipartFile.getContentType();

            if (ObjectUtils.isEmpty(contentType)) {
                // 확장자 명이 없는 경우
                throw new CustomException(CustomError.COMMUNITY_IMAGE_TYPE_ERROR);     // 이미지만 등록 가능합니다.
            } else {
                // 확장자 체크
                if (!contentType.contains("image/jpeg") && !contentType.contains("image/jpg") && !contentType.contains("image/png")) {
                    throw new CustomException(CustomError.COMMUNITY_IMAGE_TYPE_ERROR); // 이미지만 등록 가능합니다.
                }
            }
            /** 이미지 용량 체크 **/
            long size = multipartFile.getSize();
            // 이미지 크기 10MB 보다 크면
            if (size > UPLOAD_MAX_SIZE) {
                throw new CustomException(CustomError.COMMUNITY_IMAGE_SIZE_ERROR);     // 등록할 이미지 용량이 너무 큽니다.
            }

        }
    }


    /**************************************************************************************
     * Validation
     **************************************************************************************/

    /**
     * 커뮤니티 게시물 idx 유효성 검사(공통)
     *
     * @param idx
     */
    private void idxValidate(Long idx) {

        // idx 기본 유효성 검사
        if (idx == null || idx < 1L) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_IDX_EMPTY); // 요청하신 게시물을 찾을 수 없습니다.
        }

        // idx db 조회 후 검사
        int contentsCnt = communityDaoSub.getContentsCnt(idx);

        if (contentsCnt < 1) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_IDX_ERROR); // 요청하신 게시물을 찾을 수 없습니다.
        }
    }

    /**
     * 컨텐츠 idx 유효성 검사(공통)
     *
     * @param idx
     */
    private void contentIdxValidate(Integer idx) {
        // idx 기본 유효성 검사
        if (idx == null || idx < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST); // 요청하신 작품 정보를 찾을 수 없습니다.
        }

        // 조회할 episodeDto set
        ContentDto contentDto = ContentDto.builder()
                .nowDate(dateLibrary.getDatetime())// 현재 시간
                .contentsIdx(idx) // 컨텐츠 idx
                .build();

        // idx db 조회 후 검사
        int contentCnt = contentDaoSub.getContentCnt(contentDto);

        if (contentCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_EXIST); // 유효하지 않은 컨텐츠입니다.
        }
    }

    /**
     * 삭제 유효성 검사
     *
     * @param contentDto
     */
    private void deleteContentValidate(CommunityContentDto contentDto) {

        // 1. idx 유효성 검사
        idxValidate(contentDto.getIdx());
        // 로그인한 회원 IDX
        String stringMemberIdx = super.getMemberInfo(SessionConfig.IDX); // 회원 idx
        long memberIdx = Long.valueOf(stringMemberIdx);                  // Long 형변환

        // 2. 작성자 확인
        long writerIdx = communityDaoSub.getContentWriterIdx(contentDto.getIdx());

        if (memberIdx != writerIdx) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_WRITER_DIFF_ERROR); // 본인이 작성한 게시물만 삭제 가능합니다.
        }
    }

    /**
     * 게시물 리스트 유효성 검사
     *
     * @param searchDto
     */
    private void contentListValidate(SearchDto searchDto) {

        if (searchDto.getSortType() == null || searchDto.getSortType() == 0 || searchDto.getSortType() > 3) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_SORT_TYPE_ERROR); // 정렬 유형 에러
        }

        if (searchDto.getCategoryIdx() == null || searchDto.getCategoryIdx() > 4) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_CATEGORY_ERROR);  // 카테고리 에러
        }
    }

    /**
     * 컨텐츠 등록 유효성 검사
     *
     * @param contentDto
     */
    private void registerContentValidate(CommunityContentDto contentDto) {
        // 내용
        if (contentDto.getContent() == null || contentDto.getContent().isEmpty()) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_CONTENT_EMPTY); // 내용을 입력해주세요.
        }

        // 제목
        if (contentDto.getTitle() == null || contentDto.getTitle().isEmpty()) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_TITLE_EMPTY); // 제목을 입력해주세요.
        }

        // 회원 닉네임 조회
        String nickName = super.getMemberInfo(LOGIN_NICK);

        if (nickName == null || nickName.isEmpty()) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_NICK_EMPTY); // 닉네임 등록 후 게시물 등록이 가능합니다.
        }
        // 컨텐츠 길이
        if (contentDto.getContent().length() > CONTENT_MAX_SIZE) {
            throw new CustomException(CustomError.COMMUNITY_CONTENTS_LENGTH_ERROR); // 내용은 최대 500자까지 입력가능합니다.
        }

        // 컨텐츠 idx
        if (contentDto.getContentsIdx() != null && contentDto.getContentsIdx() > 0) {
            /** 컨텐츠 idx 유효성 검사 **/
            contentIdxValidate(contentDto.getContentsIdx());
            contentDto.setCategoryIdx(CONTENT_PROMOTION); // 작품 홍보
        } else {
            contentDto.setContentsIdx(0);
            contentDto.setCategoryIdx(CONTENT_NORMAL);    // 일반 게시물
        }
    }


}
