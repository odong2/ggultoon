package com.architecture.admin.controllers.v1.member;

import com.architecture.admin.config.SessionConfig;
import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.services.member.GradeService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/grade")
public class GradeV1Controller extends BaseController {

    private final GradeService gradeService;


    /**
     * 회원 등급
     * @return
     */
    @GetMapping("")
    public String memberGrade() {
        JSONObject data = gradeService.getMemberGrade();
        String message = super.langMessage("");

        return displayJson(true, "1000", message, data);
    }

    /**
     * 회원 등급 등록
     * @return
     */
    @PostMapping("")
    public String insertGrade() {
        // 회원 IDX session 체크
        super.checkSession();

        Long memberIdx = Long.valueOf(getMemberInfo(SessionConfig.IDX));
        gradeService.insertGrade(memberIdx);

        // return value
        String message = "";

        return displayJson(true, "1000", message);
    }
}
