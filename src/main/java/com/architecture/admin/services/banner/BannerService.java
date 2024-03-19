package com.architecture.admin.services.banner;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.S3Library;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.banner.BannerDao;
import com.architecture.admin.models.daosub.banner.BannerDaoSub;
import com.architecture.admin.models.dto.banner.BannerDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class BannerService extends BaseService {

    private final BannerDao bannerDao;
    private final BannerDaoSub bannerDaoSub;

    private final S3Library s3Library;

    /*
        1 - 메인페이지
        2 - 메인 바디
        3 - 뷰어
        4 - 커뮤니티리스트
        5 - 커뮤니티 내용
        6 - 충전소
        7 - 마이페이지 비로그인
        8 - 마이페이지 로그인
     */
    // 1. 720*364 - bannerType : 1, 6
    public static final Integer IMAGE_TYPE_1 = 1;

    // 2. 720*260 - bannerType : 2, 3, 5, 7, 8
    public static final Integer IMAGE_TYPE_2 = 2;

    // 3. 720*160 - bannerType : 4
    public static final Integer IMAGE_TYPE_3 = 3;

    private final List<Integer> BANNER_TYPE_1 = new ArrayList<>(Arrays.asList(new Integer[]{ 1, 6 }));
    private final List<Integer> BANNER_TYPE_2 = new ArrayList<>(Arrays.asList(new Integer[]{ 2, 3, 5, 7, 8 }));
    private final List<Integer> BANNER_TYPE_3 = new ArrayList<>(Arrays.asList(new Integer[]{ 4 }));

    /**
     * 배너 조회
     * @param bannerDto
     * @param request
     * @return
     */
    public JSONObject getBannerList(BannerDto bannerDto, HttpServletRequest request) {

        // return value
        JSONObject jsonData = new JSONObject();

        // 현재 날짜
        bannerDto.setNowDate(dateLibrary.getDatetime());

        // 성인 값 없으면 일반
        if (bannerDto.getPavilionIdx() == null) {
            bannerDto.setPavilionIdx(0);
        }

        // 로그인한 회원 정보
        Object memberInfo = session.getAttribute(SessionConfig.MEMBER_INFO);

        if (memberInfo != null) { // 로그인 상태
            JSONObject jsonMemberInfo = new JSONObject(memberInfo.toString());

            // 회원 성인 여부 체크
            Integer memberAdult = jsonMemberInfo.getInt("adult");

            if (memberAdult == 0) { // 미성년자

                // 전체(비성인+성인) 배너 열람할 경우
                if (bannerDto.getPavilionIdx() > 0) {
                    throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
                }
            }

        } else { // 비로그인 상태

            // OTT 접속한 성인 회원일 경우
            if (super.getOttVisitToken(request).equals("Y")) {
                // 비성인 + 성인 배너 전체 노출
                bannerDto.setPavilionIdx(2);

                // 전체(비성인+성인) 배너 열람할 경우
            } else if (bannerDto.getPavilionIdx() > 0) {
                throw new CustomException(CustomError.MEMBER_IS_NOT_ADULT); // 성인인증 후 이용이 가능합니다.
            }
        }

        // 노출 위치 이미지
        if (bannerDto.getType() != null) {
            if (BANNER_TYPE_1.contains(bannerDto.getType())) {
                bannerDto.setImgType(IMAGE_TYPE_1);
            }
            if (BANNER_TYPE_2.contains(bannerDto.getType())) {
                bannerDto.setImgType(IMAGE_TYPE_2);
            }
            if (BANNER_TYPE_3.contains(bannerDto.getType())) {
                bannerDto.setImgType(IMAGE_TYPE_3);
            }

            if (BANNER_TYPE_3.contains(bannerDto.getType())) {
                bannerDto.setImgType(IMAGE_TYPE_3);
            }
        } else {
            // 장르는 type = null
            if (bannerDto.getCategoryIdx() != null && bannerDto.getGenreIdx() == null) {
                // 카테고리 idx만 있으면 랭킹(0)
                bannerDto.setGenreIdx(0);
            }
            bannerDto.setImgType(IMAGE_TYPE_1);
        }

        List<BannerDto> bannerList = bannerDaoSub.getBannerList(bannerDto);

        // 이미지 fullUrl 세팅
        for (BannerDto dto : bannerList) {
            dto.setUrl(s3Library.getUploadedFullUrl(dto.getUrl()));
        }
        // list set
        jsonData.put("bannerList", bannerList);
        return jsonData;
    }

    /**
     * 배너 클릭 수 업데이트
     * @param bannerMappingIdx
     * @return
     */
    public void getBannerMapping(Integer bannerMappingIdx) {
        if (bannerMappingIdx != null) {

            // 배너 매핑 정보
            BannerDto banner = bannerDaoSub.getBannerMapping(bannerMappingIdx);

            if (banner != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                banner.setDate(formatter.format(new Date()));
                banner.setClickCount(1);

                // 배너 클릭 수 업데이트
                bannerDao.setBannerClick(banner);
            }
        }
    }
}
