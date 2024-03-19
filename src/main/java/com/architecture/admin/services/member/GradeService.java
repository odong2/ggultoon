package com.architecture.admin.services.member;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.member.GradeDao;
import com.architecture.admin.models.daosub.member.GradeDaoSub;
import com.architecture.admin.models.dto.member.MemberGradeDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@RequiredArgsConstructor
@Service
@Transactional
public class GradeService extends BaseService {

    private final GradeDaoSub gradeDaoSub;

    private final GradeDao gradeDao;


    // 등급 목록
    private ArrayList<Integer> arrayList = new ArrayList<>(Arrays.asList(10000, 50000, 100000, 150000, 200000));

    public static final List<Map<String, Object>> gradeList =  new ArrayList<>(){
        {
            add(Map.of("grade", 0, "condition", 0, "addMileage", 0, "payback", 0));
            add(Map.of("grade", 1, "condition", 10000, "addMileage", 0, "payback", 1));
            add(Map.of("grade", 2, "condition", 50000, "addMileage", 1, "payback", 2));
            add(Map.of("grade", 3, "condition", 100000, "addMileage", 2, "payback", 3));
            add(Map.of("grade", 4, "condition", 150000, "addMileage", 3, "payback", 4));
            add(Map.of("grade", 5, "condition", 200000, "addMileage", 5, "payback", 5));
        }
    };


    /**
     * 회원 등급
     * @return
     */
    public JSONObject getMemberGrade() {

        JSONObject data = new JSONObject();
        data.put("gradeList", gradeList);

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        if (memberIdx > 0) {
            // SimpleDateFormat 1일 0시 세팅
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-01 00:00:00");
            sdf.setLenient(false);
            Calendar day = Calendar.getInstance();
            // 현재시간(utc)
            String endDate = dateLibrary.getDatetime();
            // 현재월 포함 3개월전
            day.add(Calendar.MONTH, -2);
            // utc 변환
            String startDate = dateLibrary.localTimeToUtc(sdf.format(day.getTime()));
            Map<String, Object> map = new HashMap<>();
            map.put("startDate", startDate);
            map.put("endDate", endDate);
            map.put("memberIdx", memberIdx);

            // 3개월 결제 금액
            List<Map<String, Object>> monthAmount = gradeDaoSub.getMonthPayment(map);

            int val = 0;
            for (int i = 0; i < 3; i++) {
                day.add(Calendar.MONTH, val);
                String subDate = sdf.format(day.getTime()).substring(0,7);
                // 월 결제 금액 확인 - 없으면 0 추가
                if (monthAmount.stream().filter(dt -> dt.get("monthDate").equals(subDate)).findAny().isEmpty()) {
                    Map<String, Object> addMap = new HashMap<>();
                    addMap.put("amount", 0);
                    addMap.put("monthDate", subDate);
                    monthAmount.add(i, addMap);
                }
                val = 1;
            }

            data.put("amount", monthAmount);
        }

        return data;
    }

    /**
     * 회원 등급 등록
     * @return
     */
    public void insertGrade(Long memberIdx) {
        // SimpleDateFormat 1일 0시 세팅
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-01 00:00:00");
        sdf.setLenient(false);
        Calendar day = Calendar.getInstance();

        // 현재시간(utc)
        String endDate = dateLibrary.getDatetime();

        // 현재월 포함 3개월전
        day.add(Calendar.MONTH, -2);
        // utc 변환
        String startDate = dateLibrary.localTimeToUtc(sdf.format(day.getTime()));

        if (memberIdx > 0) {
            Map<String, Object> map = new HashMap<>();
            map.put("startDate", startDate);
            map.put("endDate", endDate);
            map.put("memberIdx", memberIdx);

            // 결제 금액
            Integer amount = gradeDao.getMemberPayment(map);

            // 결제금액 등급 정보
            MemberGradeDto memberGradeDto = myGrade(amount);
            if (memberGradeDto.getGrade() != null) {
                memberGradeDto.setIdx(memberIdx);
                // 회원 등급 등록
                gradeDao.insertMemberGrade(memberGradeDto);
            }
        } else {
            throw new CustomException(CustomError.MEMBER_IDX_ERROR); // 로그인 후 이용해주세요.
        }
    }

    /**
     * 회원 등급 레벨 조회
     * @param memberIdx
     * @return
     */
    public Integer getMemberGradeLevel(Long memberIdx) {
        // 회원 등급
        MemberGradeDto memberGradeDto = gradeDaoSub.getMemberGrade(memberIdx);
        Integer grade = 0;
        if (memberGradeDto != null) {
            // 회원 등급 레벨 조회
            grade = memberGradeDto.getGrade();
        }

        return grade;
    }


    /**
     * 결제금액 등급 정보
     * @param memberPayment
     * @return
     */
    private MemberGradeDto myGrade(Integer memberPayment) {

        MemberGradeDto dto = new MemberGradeDto();
        for (Map<String, Object> grade : gradeList) {
            if (memberPayment >= Integer.parseInt(grade.get("condition").toString())) {
                dto.setAmount(memberPayment);
                dto.setGrade(Integer.parseInt(grade.get("grade").toString()));
                dto.setCondition(Integer.parseInt(grade.get("condition").toString()));
                dto.setAddMileage(Integer.parseInt(grade.get("addMileage").toString()));
                dto.setPayback(Integer.parseInt(grade.get("payback").toString()));
            }
        }
        return dto;
    }

}
