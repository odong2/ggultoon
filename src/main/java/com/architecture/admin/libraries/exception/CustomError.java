package com.architecture.admin.libraries.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.EnumSet;

/**
 ******* 오류코드 작성 규칙 ******
 *      - 영문4자 와  숫자4자리로 구성 ex) ELGI-9999
 *      - 앞4자리 영문은 기능이나 페이지를 알 수 있도록 작성
 *      - 뒤4자리 숫자는 아래 규칙에 따라 분류
 *       오류번호   /   설명
 *        1000    =   정상
 *        2xxx    =   필수값 없음
 *        3xxx    =   유효성오류
 *        4xxx    =   sql구문오류
 *        5xxx    =   DB데이터오류
 *        6xxx    =   파일오류
 *        7xxx    =   권한오류
 *        9xxx    =   기타오류
 */
public enum CustomError {

    // EBAD : 유저의 잘못된 요청
    BAD_REQUEST("EBAD-3999", "lang.common.exception.bad.request")                            // 잘못된 요청입니다.(bad request 공통)
    , BAD_REQUEST_PARAMETER_TYPE_MISMATCH("EBAD-3998", "lang.common.exception.bad.request")  // 잘못된 요청입니다.(메소드로 넘어오는 파라미터의 타입 미스매치등)
    , BAD_REQUEST_REQUIRED_VALUE("EBAD-3997", "lang.common.exception.bad.required.value")    // 필수값을 입력해주세요.

    // ESER : 서버 오류(SQL,DB)
    , SERVER_NORMAL_ERROR("ESER-9999", "lang.common.exception.server")     // 죄송합니다.서버에 문제가 발생했습니다.잠시후 다시 이용해주세요.
    , SERVER_DATABASE_ERROR("ESER-5999", "lang.common.exception.server")   // 죄송합니다.서버에 문제가 발생했습니다.잠시후 다시 이용해주세요.
    , SERVER_SQL_ERROR("ESER-5998", "lang.common.exception.server")        // 죄송합니다.서버에 문제가 발생했습니다.잠시후 다시 이용해주세요.

    // EMEM : 회원 관련 오류
    , MEMBER_IDX_ERROR("EMEM-9999", "lang.member.exception.idx")            // 로그인 후 이용해주세요.
    , MEMBER_IS_NOT_ADULT("EMEM-9998", "lang.member.exception.adult")       // 성인인증 후 이용이 가능합니다.

    // EFIN : 본인인증 관련 오류
    , FIND_MEMBER_ID_NOT_EXIST("EFIN-9999", "lang.member.exception.find.id")                      // 아이디를 찾을 수 없습니다.
    , FIND_MEMBER_PASSWORD_NOT_EXIST("EFIN-9998", "lang.member.exception.find.password")          // 비밀번호를 찾을 수 없습니다.
    , FIND_MEMBER_PASSWORD_RESET_FAIL("EFIN-9997", "lang.member.exception.reset.password.fail")   // 비밀번호를 재설정할 수 없습니다.
    , FIND_MEMBER_IDENTIFICATION_FAIL("EFIN-9996", "lang.member.exception.identification")        // 본인인증에 실패했습니다.

    // ELGI : 로그인 관련 오류
    , LOGIN_FAIL("ELGI-9999", "lang.login.exception.login_fail")            // 로그인이 실패하였습니다.
    , MEMBER_STATE_ERROR("ELGI-3999", "lang.login.exception.state")         // 계정상태를 확인해주세요.
    , LOGIN_ID_ERROR("ELGI-2999", "lang.login.exception.id_null")           // 아이디를 입력해주세요
    , LOGIN_PW_ERROR("ELGI-2998", "lang.login.exception.pw")                // 패스워드를 입력해주세요

    // ELGO : 로그아웃 관련 오류
    , LOGOUT_FAIL("ELGO-9999", "lang.login.exception.logout_fail")          // 로그아웃에 실패하였습니다.

    // EJOI : 회원가입 관련 오류
    , JOIN_FAIL("EJOI-9999", "lang.login.exception.join_fail")              // 회원가입에 실패하였습니다.
    , JOIN_NICK_FAIL("EJOI-9998", "lang.login.exception.nik_fail")          // 닉네임 등록 실패
    , JOIN_ID_ERROR("EJOI-2999", "lang.login.exception.id_null")            // 아이디를 입력해주세요
    , JOIN_PW_ERROR("EJOI-2998", "lang.login.exception.pw")                 // 비밀번호를 입력해주세요
    , JOIN_SOCIAL_ERROR("EJOI-2997", "lang.login.exception.social_fail")    // 소셜 코드 조회 실패
    , GOOGLE_SOCIAL_ERROR("EJOI-2996", "lang.login.exception.google_fail")  // 구글 조회 실패
    , NAVER_SOCIAL_ERROR("EJOI-2995", "lang.login.exception.naver_fail")    // 네이버 조회 실패
    , JOIN_POLICY_EMPTY("EJOI-2994", "lang.login.exception.policy_empty")   // 약관 필수값이 넘어오지 않았습니다
    , ID_DUPLE("EJOI-3998", "lang.login.exception.id_duple")                // 이미 존재하는 아이디입니다.
    , JOIN_ID_EMAIL_ERROR("EJOI-3997", "lang.login.exception.id_email")     // 아이디는 이메일로 입력해주세요.
    , SIMPLE_JOIN_ERROR("EJOI-3996", "lang.login.exception.simple_join")    // 간편 가입으로 진입해 주세요
    , UUID_DUPLE("EJOI-3995", "lang.login.exception.uuid_duple")            // 이미 존재하는 아이디입니다.
    , JOIN_POLICY_MUST("EJOI-3994", "lang.login.exception.policy_must")     // 약관 필수값에 동의해주세요
    , NAME_CHECK_ERROR("EJOI-2993", "lang.login.exception.txseq")           // 본인인증이 필요합니다.

    // ENIC : 닉네임 관련 오류
    , NICK_EMPTY("ENIC-2999", "lang.member.exception.nick_empty")                // 닉네임값이 비어있습니다.
    , NICK_LENGTH_ERROR("ENIC-3999", "lang.member.exception.nick_length")        // 최소 3자 이상 최대 8자 이하만 입력할 수 있습니다.
    , NICK_STRING_ERROR("ENIC-3998", "lang.member.exception.nick_string")        // 사용할 수 없는 문자가 포함되어 있습니다.
    , NICK_DUPLE("ENIC-3997", "lang.member.exception.nick_duple")                // 이미 존재하는 닉네임입니다.
    , NICK_CHECK_FAIL("ENIC-3996", "lang.member.exception.nick_check")           // 닉네임 검사 실패
    , NICK_HAVE("ENIC-3995", "lang.member.exception.nick_have")                  // 이미 닉네임을 등록한 회원입니다

    // EMMF : 회원 정보 변경 관련 오류
    , MEMBER_DELETE_FAIL("EMMF-9999", "lang.member.exception.delete.fail")                              // 회원 탈퇴에 실패하였습니다.
    , DELETE_BENEFIT_FAIL("EMMF-9998", "lang.member.exception.delete.benefit")                          // 특별 혜택 받기에 실패하였습니다.
    , MEMBER_INFO_UPDATE_FAIL("EMMF-5999", "lang.member.exception.delete.fail")                         // 회원 탈퇴에 실패하였습니다.
    , ID_EMPTY("EMMF-2999", "lang.member.exception.id")                                                 // 아이디를 입력해주세요.
    , NEW_PASSWORD_EMPTY("EMMF-2998", "lang.member.exception.modify.new.password.empty")                // 변경할 비밀번호를 입력해주세요.
    , NEW_PASSWORD_CONFIRM_EMPTY("EMMF-2997", "lang.member.exception.modify.new.passwordConfirm.empty") // 비밀번호 확인란을 입력해주세요.
    , PASSWORD_CORRESPOND_ERROR("EMMF-3999", "lang.member.exception.modify.password.notEquals")         // 이전 비밀번호와 같아요.
    , PASSWORD_CONFIRM("EMMF-3998", "lang.login.exception.pw_confirm")                                  // 비밀번호를 동일하게 입력해주세요.
    , PASSWORD_PATTERN_NOT_MATCH("EMMF-3997", "lang.member.exception.modify.pw_pattern")                // 비밀번호 형식에 맞게 입력해주세요.
    , ID_CORRESPOND_ERROR("EMMF-3996", "lang.member.exception.id.notEquals")                            // 입력하신 아이디를 찾을 수 없습니다.
    , ALREADY_DELETE_BENEFIT("EMMF-3995", "lang.member.exception.already.delete.benefit")               // 이미 특별 혜택을 받으셨습니다.
    , SIMPLE_MEMBER_PASSWORD_ERROR("EMMF-3994", "lang.member.exception.simple.modify.password")         // 소셜 가입 회원은 비밀번호를 변경할 수 없습니다.

    // EPUR : 회차 구매 관련 오류
    , PURCHASE_EPISODE_BUY_FAIL("EPUR-9999", "lang.purchase.exception.buy_fail")                     // 회차구매에 실패하였습니다.
    , PURCHASE_DELETE_FAIL("EPUR-9998", "lang.purchase.exception.delete_fail")                       // 구매내역 삭제에 실패하였습니다.
    , PURCHASE_IDX_DUPLE_ERROR("EPUR-5999", "lang.purchase.exception.buy_fail")                      // 회차구매에 실패하였습니다.
    , PURCHASE_MEMBER_COIN_UPDATE_FAIL("EPUR-5998", "lang.purchase.exception.buy_fail")              // 회차구매에 실패하였습니다.
    , PURCHASE_TYPE_ERROR("EPUR-3999", "lang.purchase.exception.type")                               // 구매유형이 올바르지 않습니다.
    , PURCHASE_IDX_ERROR("EPUR-3998", "lang.purchase.exception.idx")                                 // 유효하지 않은 구매내역입니다.
    , PURCHASE_ALREADY_ALL_BUY("EPUR-3997", "lang.purchase.exception.already.all.buy")               // 이미 모든 회차를 보유중입니다.
    , PURCHASE_RENT_NOT_EXIST("EPUR-3996","lang.purchase.exception.rent.not.exist")                  // 대여할 회차가 없습니다.
    , PURCHASE_HAVE_NOT_EXIST("EPUR-3995","lang.purchase.exception.have.not.exist")                  // 소장할 회차가 없습니다.
    , PURCHASE_ALREADY_RENT("EPUR-3994", "lang.purchase.exception.already.rent")                     // 이미 대여중인 회차입니다.
    , PURCHASE_ALREADY_HAVE("EPUR-3993","lang.purchase.exception.already.have")                      // 이미 소장중인 회차입니다.
    , PURCHASE_INCLUDE_FREE_NOT_MATCH("EPUR-3992", "lang.purchase.exception.include.free.not.match") // 무료 회차 포함여부가 불명확합니다.
    , PURCHASE_ROUTE_ERROR("EPUR-3991", "lang.purchase.exception.route")                             // 잘못된 구매 경로입니다.
    , PURCHASE_CANT_BUY_EVENT("EPUR-3990","lang.purchase.exception.cant.buy.event")                  // 본 회차는 이벤트로 인해 무료이므로 구매가 불가합니다.
    , PURCHASE_CANT_BUY_FREE("EPUR-3989", "lang.purchase.exception.cant.buy.free")                   // 본 회차는 무료로 개별 구매가 불가합니다.

    // ECON : 컨텐츠 관련 오류
    , CATEGORY_IDX_EMPTY("ECON-2999", "lang.contents.exception.category")                               // 요청하신 카테고리 정보를 찾을 수 없습니다.
    , CONTENTS_IDX_EMPTY("ECON-2998", "lang.contents.exception.content")                                // 요청하신 작품 정보를 찾을 수 없습니다.
    , CONTENTS_DELETE_IDX_EMPTY("ECON-2997", "lang.contents.exception.delete.empty")                    // 삭제할 작품이 없습니다.
    , CONTENTS_SEARCH_WORD_EMPTY("ECON-2996", "lang.contents.exception.search.word")                    // 검색어를 입력해주세요.
    , CONTENTS_COMMENT_IDX_EMPTY("ECON-2995", "lang.contents.exception.comment")                        // 존재하지 않는 댓글입니다.
    , CONTENTS_COMMENT_CONTENT_EMPTY("ECON-2994", "lang.contents.exception.comment.empty")              // 댓글 내용을 입력해주세요.
    , CONTENTS_COMMENT_NICK_EMPTY("ECON-2993", "lang.contents.exception.nick.empty")                    // 닉네임 등록 후 댓글 이용이 가능합니다.
    , CONTENTS_CURATION_EMPTY("ECON-2992", "lang.contents.exception.curation.empty")                    // 큐레이션 노출 영역을 선택해주세요.
    , PAVILION_IDX_EMPTY("ECON-2991", "lang.contents.exception.pavilion")                               // 요청하신 이용관 정보를 찾을 수 없습니다.
    , CATEGORY_NOT_EXIST("ECON-3999", "lang.contents.exception.category")                               // 요청하신 카테고리 정보를 찾을 수 없습니다.
    , CONTENTS_NOT_EXIST("ECON-3998", "lang.contents.exception.content")                                // 요청하신 작품 정보를 찾을 수 없습니다.
    , CONTENTS_EPISODE_NOT_EXIST("ECON-3997", "lang.contents.exception.episode")                        // 요청하신 회차 정보를 찾을 수 없습니다.
    , GENRE_NOT_EXIST("ECON-3996", "lang.contents.exception.genre")                                     // 요청하신 장르 정보를 찾을 수 없습니다.
    , SORT_NOT_EXIST("ECON-3995", "lang.contents.exception.sort")                                       // 요청하신 정렬 정보를 찾을 수 없습니다.
    , ADULT_FILTER_ERROR("ECON-3994", "lang.contents.exception.adult")                                  // 요청하신 성인 작품 정보를 찾을 수 없습니다.
    , CONTENTS_REPORT_NOT_EXIST("ECON-3993", "lang.contents.exception.report.empty")                    // 신고를 취소할 신고 건이 없습니다.
    , CONTENTS_COMMENT_NOT_EXIST("ECON-3992", "lang.contents.exception.comment")                        // 존재하지 않는 댓글입니다.
    , CONTENTS_COMMENT_LENGTH_ERROR("ECON-3991", "lang.contents.exception.comment.length")              // 최대 200자까지 입력이 가능합니다.
    , CONTENTS_COMMENT_WRITER_DIFF("ECON-3990", "lang.contents.exception.comment.writer.diff")          // 본인이 작성한 댓글만 삭제가 가능합니다.
    , CONTENTS_FAVORITE_NOT_EXIST("ECON-3989", "lang.contents.exception.favorite.empty")                // 찜하지 않은 작품입니다.
    , CONTENTS_CURATION_NOT_EXIST("ECON-3988", "lang.contents.exception.curation")                      // 요청하신 노출 영역을 찾을 수 없습니다.
    , CONTENTS_REPORT_DUPLE_ERROR("ECON-5999", "lang.contents.exception.report.duple")                  // 이미 신고한 작품입니다.
    , CONTENTS_COMMENT_REPORT_DUPLE_ERROR("ECON-5998", "lang.contents.exception.report.comment.duple")  // 이미 신고한 댓글입니다.
    , CONTENTS_FAVORITE_DUPLE_ERROR("ECON-5997", "lang.contents.exception.favorite.content.duple")      // 이미 찜한 작품입니다.
    , FAVORITE_DELETE_FAIL("ECON-9999","lang.contents.exception.favorite.delete.fail")                  // 관심 작품 삭제에 실패하였습니다.
    , CONTENTS_VIEW_DELETE_FAIL("ECON-9998", "lang.contents.exception.view.delete.fail")                // 최근 본 작품 삭제에 실패하였습니다.
    , CONTENTS_FAVORITE_ERROR("ECON-9997", "lang.contents.exception.favorite")                          // 관심 작품 목록에 추가할 수 없습니다.
    , CONTENTS_FAVORITE_CANCEL_ERROR("ECON-9996", "lang.contents.exception.favorite.cancel")            // 관심 작품 목록에서 삭제할 수 없습니다.
    , CONTENTS_REPORT_ERROR("ECON-9995", "lang.contents.exception.report")                              // 작품을 신고할 수 없습니다.
    , CONTENTS_COMMENT_REGISTER_ERROR("ECON-9994", "lang.contents.exception.register.comment")          // 댓글 등록을 실패하였습니다.
    , CONTENTS_COMMENT_DELETE_ERROR("ECON-9993", "lang.contents.exception.delete.comment")              // 댓글 삭제를 실패하였습니다.
    , CONTENTS_COMMENT_MODIFY_ERROR("ECON-9992", "lang.contents.exception.modify.comment")              // 댓글 수정를 실패하였습니다.
    , CONTENTS_COMMENT_LIKE_ERROR("ECON-9991", "lang.contents.exception.comment.like")                  // 댓글 좋아요를 실패하였습니다.
    , CONTENTS_COMMENT_LIKE_CANCEL_ERROR("ECON-9990", "lang.contents.exception.comment.like.cancel")    // 댓글 좋아요 취소를 실패하였습니다.
    , CONTENTS_COMMENT_REPORT_ERROR("ECON-9989", "lang.contents.exception.comment.report.cancel")       // 댓글을 신고할 수 없습니다.
    , CONTENTS_RANK_INSERT_ERROR("ECON-9988", "lang.contents.exception.rank.insert")                    // 작품 랭킹 정보를 등록할 수 없습니다.
    , CONTENTS_RANK_UPDATE_ERROR("ECON-9987", "lang.contents.exception.rank.update")                    // 작품 랭킹 정보를 업데이트할 수 없습니다.
    , CONTENTS_RANK_UPDATE_EMPTY("ECON-9986", "lang.contents.exception.rank.update.empty")              // 업데이트할 작품 랭킹 정보가 없습니다.
    , CONTENTS_RANK_DELETE_ERROR("ECON-9985", "lang.contents.exception.rank.delete")                    // 이전 작품 랭킹 정보를 삭제할 수 없습니다.
    , CONTENTS_RANK_DELETE_EMPTY("ECON-9984", "lang.contents.exception.rank.delete.empty")              // 삭제할 이전 작품 랭킹 정보가 없습니다.
    , CONTENTS_REPLY_LIKE_ERROR("ECON-9983", "lang.contents.exception.reply.like")                      // 대댓글에는 좋아요를 등록할 수 없습니다.

    // EEPI : 회차 관련 오류
    , EPISODE_IDX_ERROR("EEPI-9999", "lang.episode.exception.idx")                                   // 유효하지 않은 회차입니다.
    , EPISODE_COMMENT_IDX_ERROR("EPPI-9998", "lang.episode.exception.comment.not.exist")             // 존재하지 않는 댓글입니다.
    , EPISODE_COMMENT_REGIST_FAIL("EPPI-9997", "lang.episode.exception.comment.regist.fail")         // 댓글 등록에 실패하였습니다.
    , EPISODE_COMMENT_LIKE_FAIL("EPPI-9996", "lang.episode.exception.comment.like")                  // 댓글 좋아요를 실패하였습니다.
    , EPISODE_COMMENT_LIKE_CANCEL_FAIL("EPPI-9995", "lang.episode.exception.comment.like.cancel")    // 댓글 좋아요 취소를 실패하였습니다.
    , EPISODE_COMMENT_MODIFY_FAIL("EPPI-9994", "lang.episode.exception.comment.modify.fail")         // 댓글 수정을 실패하였습니다.
    , EPISODE_COMMENT_DELETE_FAIL("EPPI-9993", "lang.episode.exception.comment.delete.fail")         // 댓글 삭제를 실패하였습니다.
    , EPISODE_COMMENT_REPORT_FAIL("EPPI-9992", "lang.episode.exception.comment.report.fail")         // 댓글 신고를 실패하였습니다.
    , EPISODE_COMMENT_IDX_EMPTY("EPPI-2999","lang.episode.exception.comment.idx.empty")              // 존재하지 않는 댓글입니다.
    , EPISODE_COMMENT_CONTENT_EMPTY("EPPI-2998", "lang.episode.exception.comment.content.empty")     // 댓글 내용을 입력해주세요.
    , EPISODE_COMMENT_LENGTH_ERROR("EPPI-3999", "lang.episode.exception.comment.length.error")       // 최대 200자 까지 입력 가능합니다.
    , EPISODE_COMMENT_NICK_EMPTY("EPPI-3998", "lang.episode.exception.comment.nick.empty")           // 닉네임 설정 후 작성해주세요.
    , EPISODE_COMMENT_WRITER_DIFF("EPPI-3997","lang.episode.exception.comment.writer.diff" )         // 본인 작성 댓글이 아닙니다.
    , EPISODE_COMMENT_REPORT_EXIST("EPPI-3996", "lang.episode.exception.comment.report.exist")       // 이미 신고한 댓글입니다.
    , EPISODE_VIEWER_NOT_PURCHASE("EPPI-3899","lang.episode.exception.viewer.not.purchase")          // 구매 후 이용이 가능합니다.

    // ECOI : 코인 관련 오류
    , HAVE_COIN_LACK("ECOI-3999", "lang.coin.exception.have.lack")             // 보유 코인이 부족합니다.
    , COIN_PURCHASE_LOCK("ECOI-3998", "lang.coin.exception.service.shutdown")  // 서비스 종료로 코인 사용이 불가능합니다.

    // ESEA : 검색 관련 오류
    , SEARCH_TYPE_EMPTY("ESEA-2999", "lang.common.exception.search.type.empty")     // 검색 유형을 선택해주세요.
    , SEARCH_TYPE_ERROR("ESEA-3999", "lang.common.exception.search.type")           // 요청하신 검색 유형을 찾을 수 없습니다.

    // ENOT : 알림 관련 오류
    , NOTIFICATION_IDX_ERROR("ENOT-2999", "lang.notification.exception.idx.empty")              // 알림을 선택해주세요.
    , NOTIFICATION_DELETE_IDX_EMPTY("ENOT-2998", "lang.notification.exception.delete.empty")    // 삭제할 알림이 없습니다.
    , NOTIFICATION_IDX_NOT_EXIST("ENOT-3999", "lang.notification.exception.idx")                // 요청하신 알림 정보를 찾을 수 없습니다.
    , NOTIFICATION_DELETED("ENOT-3998", "lang.notification.exception.deleted")                  // 이미 삭제된 알림입니다.
    , NOTIFICATION_CHECK_FAIL("ENOT-9999", "lang.notification.exception.check.fail")            // 알림을 확인할 수 없습니다.
    , NOTIFICATION_DELETE_FAIL("ENOT-9998", "lang.notification.exception.delete.fail")          // 알림을 삭제할 수 없습니다.

    // ESET : 환경설정 관련 오류
    , SETTING_IDX_ERROR("ESET-2999", "lang.setting.exception.idx.empty")        // 변경할 환경설정 옵션을 선택해주세요.
    , SETTING_IDX_NOT_EXIST("ESET-3999", "lang.setting.exception.idx")          // 변경할 환경설정 옵션을 찾을 수 없습니다.
    , SETTING_SWITCH_FAIL("ESET-9999", "lang.setting.exception.switch.fail")    // 환경설정 상태를 변경할 수 없습니다.
    , SETTING_SWITCH_DUPLE("ESET-5999", "lang.setting.exception.switch.duple")  // 해당 설정이 이미 적용된 상태입니다.

    // ECOM : 커뮤니티 관련 오류
    , COMMUNITY_COMMENT_REGIST_FAIL("ECOM-9999", "lang.community.exception.comment.regist.fail")            // 댓글 등록에 실패하였습니다.
    , COMMUNITY_COMMENT_INFO_REGIST_FAIL("ECOM-9998", "lang.community.exception.comment.regist.fail")       // 댓글 등록에 실패하였습니다.
    , COMMUNITY_COMMENT_DELETE_FAIL("ECOM-9997", "lang.community.exception.comment.delete.fail")            // 댓글 삭제에 실패하였습니다.
    , COMMUNITY_COMMENT_INFO_DELETE_FAIL("ECOM-9996", "lang.community.exception.comment.delete.fail")       // 댓글 삭제에 실패하였습니다.
    , COMMUNITY_COMMENT_REPORT_FAIL("ECOM-9995", "lang.community.exception.comment.report.fail")            // 댓글 신고에 실패하였습니다.
    , COMMUNITY_COMMENT_LIKE_FAIL("ECOM-9994", "lang.community.exception.comment.like.fail")                // 댓글 좋아요를 실패하였습니다.
    , COMMUNITY_COMMENT_LIKE_CANCEL_FAIL("ECOM-9993", "lang.community.exception.comment.like.cancel.fail")  // 댓글 좋아요 취소를 실패하였습니다.
    , COMMUNITY_CONTENT_REPORT_FAIL("ECOM-9992", "lang.community.exception.content.report.fail")            // 게시물 신고를 실패하였습니다.
    , COMMUNITY_CONTENTS_LIKE_FAIL("ECOM-9991","lang.community.exception.content.like.fail")                // 게시물 좋아요를 실패하였습니다.
    , COMMUNITY_CONTENTS_LIKE_CANCEL_FAIL("ECOM-9990","lang.community.exception.content.like.cancel.fail")  // 게시물 좋아요 취소를 실패하였습니다.
    , COMMUNITY_CONTENTS_REGISTER_ERROR("ECOM-9989", "lang.community.exception.content.register.fail")      // 게시물 등록에 실패하였습니다.
    , COMMUNITY_CONTENTS_DELETE_FAIL("ECOM-9988", "lang.community.exception.content.delete.fail")           // 게시물 삭제를 실패하였습니다.
    , COMMUNITY_CONTENTS_IDX_EMPTY("ECOM-2999", "lang.community.exception.idx")                             // 요청하신 게시물을 찾을 수 없습니다.
    , COMMUNITY_COMMENT_CONTENT_EMPTY("ECOM-2998", "lang.community.exception.comment.content.empty" )       // 댓글 내용을 입력해주세요.
    , COMMUNITY_COMMENT_IDX_EMPTY("ECOM-2997", "lang.community.exception.comment.idx.empty")                // 존재하지 않는 댓글입니다.
    , COMMUNITY_CONTENTS_CONTENT_EMPTY("ECOM-2996", "lang.community.exception.content.empty")               // 내용을 입력해주세요.
    , COMMUNITY_CONTENTS_TITLE_EMPTY("ECOM-2995", "lang.community.exception.content.title.empty")           // 제목을 입력해주세요.
    , COMMUNITY_CONTENTS_NICK_EMPTY("ECOM-2994", "lang.community.exception.content.nick.empty")             // 닉네임 등록 후 게시물 등록이 가능합니다.
    , COMMUNITY_CONTENTS_IDX_ERROR("ECOM-3999", "lang.community.exception.idx")                             // 요청하신 게시물을 찾을 수 없습니다.
    , COMMUNITY_COMMENT_LENGTH_ERROR("ECOM-3998", "lang.community.exception.comment.length.error")          // 최대 200자까지 입력이 가능합니다.
    , COMMUNITY_COMMENT_NICK_EMPTY("ECOM-3997", "lang.community.exception.comment.nick.empty")              // 닉네임 설정 후 작성해주세요.
    , COMMUNITY_COMMENT_IDX_ERROR("ECOM-3996", "lang.community.exception.comment.not.exist")                // 존재하지 않는 댓글입니다.
    , COMMUNITY_COMMENT_WRITER_DIFF("ECOM-3995", "lang.community.exception.comment.writer.diff")            // 본인작성 댓글이 아닙니다.
    , COMMUNITY_COMMENT_REPORT_EXIST("ECOM-3994", "lang.community.exception.comment.report.exist")          // 이미 신고한 댓글입니다.
    , COMMUNITY_CONTENT_REPORT_EXIST("ECOM-3993", "lang.community.exception.content.report.exist")          // 이미 신고한 게시물입니다.
    , COMMUNITY_IMAGE_TYPE_ERROR("ECOM-3992", "lang.community.exception.content.image.type")                // 이미지만 등록 가능합니다.
    , COMMUNITY_IMAGE_SIZE_ERROR("ECOM-3991", "lang.community.exception.content.image.size")                // 등록할 이미지 용량이 너무 큽니다.
    , COMMUNITY_CONTENTS_LENGTH_ERROR("ECOM-3990","lang.community.exception.content.length")                // 내용은 최대 500자까지 입력가능합니다.
    , COMMUNITY_CONTENTS_SORT_TYPE_ERROR("ECOM-3989", "lang.community.exception.content.sort.type")         // 정렬 유형이 올바르지 않습니다.
    , COMMUNITY_CONTENTS_CATEGORY_ERROR("ECOM-3988", "lang.community.exception.content.category")           // 카테고리 유형이 올바르지 않습니다.
    , COMMUNITY_CONTENTS_WRITER_DIFF_ERROR("ECOM-3987", "lang.community.exception.content.writer.different")// 본인이 작성한 게시물만 삭제 가능합니다.

    // EBOR : 게시판 관련 오류
    , BOARD_NOTICE_IDX_EMPTY("EBOR-2999", "lang.board.exception.notice.idx.empty")  // 요청하신 공지사항을 찾을 수 없습니다.
    , BOARD_NOTICE_IDX_ERROR("EBOR-3999", "lang.board.exception.notice.idx.error")  // 요청하신 공지사항을 찾을 수 없습니다.

    // EEVR : 이벤트 관련 오류
    , EVENT_COUPON_CODE_ERROR("EEVR-3999", "lang.event.coupon.code")  // 쿠폰코드를 확인해주세요.
    , EVENT_DUPLE_ERROR("EEVR-3998", "lang.event.duple")  // 이벤트 참여내역이 있습니다.
    , EVENT_END("EEVR-3997", "lang.event.end")  // 종료된 이벤트입니다. 다음 이벤트를 기다려 주세요.
    ;



    @Autowired
    MessageSource messageSource;
    private String code;
    private String message;

    CustomError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return messageSource.getMessage(message, null, LocaleContextHolder.getLocale());
    }

    public CustomError setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
        return this;
    }

    @Component
    public static class EnumValuesInjectionService {

        @Autowired
        private MessageSource messageSource;

        // bean
        @PostConstruct
        public void postConstruct() {
            for (CustomError customError : EnumSet.allOf(CustomError.class)) {
                customError.setMessageSource(messageSource);
            }
        }
    }
}
