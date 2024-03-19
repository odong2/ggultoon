package com.architecture.admin.libraries.jwt;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/*****************************************************
 * JWT 라이브러리
 ****************************************************/
@Component
public class JwtLibrary {
    private static String secretKey = Base64.getEncoder().encodeToString("petSnsApi".getBytes()); // static 해야함 들고있어야해서 여러개쓸때는 여러개만들어놓고, 인자값에서는 구분값가져와서 각자 다른 키 사용하도록
    private static String secretKey1 = Base64.getEncoder().encodeToString("petSnsApi1".getBytes()); // static 해야함 들고있어야해서 여러개쓸때는 여러개만들어놓고, 인자값에서는 구분값가져와서 각자 다른 키 사용하도록

    private JwtDto jwtDto = new JwtDto();

    /**
     * JWT 토큰 생성
     *
     * @param secretKeyType
     * @param tokenMap
     * @return
     */
    public JwtDto createToken(String secretKeyType, HashMap<String, Object> tokenMap) {

        Claims claims = Jwts.claims().setSubject(secretKeyType); // JWT payload 에 저장되는 정보단위
        claims.put("tokenMap", tokenMap); // 정보는 key / value 쌍으로 저장된다.
        Date now = new Date();
        String secretKeyUse = "";
        if ("normal".equals(secretKeyType)) {
            secretKeyUse = secretKey;
        } else if ("test".equals(secretKeyType)) {
            secretKeyUse = secretKey1;
        }

        //Access Token
        String accessToken = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + jwtDto.getValidTime())) // set Expire Time
                .signWith(SignatureAlgorithm.HS256, secretKeyUse)  // 사용할 암호화 알고리즘, signature 에 들어갈 secret값 세팅
                .compact(); // 위 설정대로 JWT 토큰 생성

        //Refresh Token
        String refreshToken = Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + jwtDto.getValidTimeRefresh())) // set Expire Time
                .signWith(SignatureAlgorithm.HS256, secretKeyUse)  // 사용할 암호화 알고리즘, signature 에 들어갈 secret값 세팅
                .compact(); // 위 설정대로 JWT 토큰 생성

        return JwtDto.builder().accessToken(accessToken).refreshToken(refreshToken).key(secretKeyType).build();
    }

    /**
     *  access token 검증
     *
     * @param secretKeyType
     * @param accessTokenObj
     * @return
     */
    public Integer validateAccessToken(String secretKeyType, JwtDto accessTokenObj) {

        // refresh 객체에서 refreshToken 추출
        String accessToken = accessTokenObj.getAccessToken();
        try {
            String secretKeyUse = "";
            if ("normal".equals(secretKeyType)) {
                secretKeyUse = secretKey;
            } else if ("test".equals(secretKeyType)) {
                secretKeyUse = secretKey;
            }
            // 토큰 자체가 유효한 내용인지 검증
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKeyUse).parseClaimsJws(accessToken);
            // 유효하며 access 토큰의 만료시간이 지나지 않았을 경우
            if (!claims.getBody().getExpiration().before(new Date())) {
                return 1;
            } else if (claims.getBody().getExpiration().before(new Date())) {// 유효하나 기간이 만료일경우
                return 2;
            }
        } catch (ExpiredJwtException e) {
            // 기간 만료
            return 2;
        } catch (SignatureException e) {
            // signature 인증 실패
            return 0;
        } catch (Exception e) {
            // access 토큰 에러(검증실패)
            return 0;
        }
        return 0; // 검증완료 - 도달할 경우 없음
    }

    /**
     * refresh token 검증
     *
     * @param secretKeyType
     * @param refreshTokenObj
     * @return
     */
    public Integer validateRefreshToken(String secretKeyType, JwtDto refreshTokenObj) {

        // refresh 객체에서 refreshToken 추출
        String refreshToken = refreshTokenObj.getRefreshToken();
        try {
            String secretKeyUse = "";
            if ("normal".equals(secretKeyType)) {
                secretKeyUse = secretKey;
            } else if ("test".equals(secretKeyType)) {
                secretKeyUse = secretKey;
            }
            // 토큰 자체가 유효한지 검증
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKeyUse).parseClaimsJws(refreshToken);
            // refresh 토큰의 만료시간이 지나지 않았을 경우
            if (!claims.getBody().getExpiration().before(new Date())) {
                return 1;
            }
        } catch (ExpiredJwtException e) {
            // 기간 만료
            return 2;
        } catch (SignatureException e) {
            // signature 인증 실패
            return 0;
        } catch (Exception e) { // refresh토큰 검증 오류(실패)
            // refresh 토큰이 만료되었을 경우, 토큰 완전 재발급이 필요합니다.
            return 0;
        }
        return 0; //검증완료-실제로 도달하지않음
    }

    /**
     * access token 재발급 (refresh token이 유효한 경우 호출됨)
     *
     * @param secretKeyType
     * @param tokenMap
     * @return
     */
    public String recreationAccessToken(String secretKeyType, Object tokenMap) {

        Claims claims = Jwts.claims().setSubject(secretKeyType); // JWT payload 에 저장되는 정보단위
        claims.put("tokenMap", tokenMap); // 정보는 key / value 쌍으로 저장된다.
        Date now = new Date();
        String secretKeyUse = "";
        if ("normal".equals(secretKeyType)) {
            secretKeyUse = secretKey;
        } else if ("test".equals(secretKeyType)) {
            secretKeyUse = secretKey1;
        }

        //Access Token
        return Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + jwtDto.getValidTime())) // set Expire Time
                .signWith(SignatureAlgorithm.HS256, secretKeyUse)  // 사용할 암호화 알고리즘과
                // signature 에 들어갈 secret값 세팅
                .compact();
    }

    /**
     * refreshToken 재발급 (refresh token이 유효시간 갱신이 필요한 경우 호출됨)
     *
     * @param secretKeyType
     * @param tokenMap
     * @return
     */
    public String recreationRefreshToken(String secretKeyType, Object tokenMap) {

        Claims claims = Jwts.claims().setSubject(secretKeyType); // JWT payload 에 저장되는 정보단위
        claims.put("tokenMap", tokenMap); // 정보는 key / value 쌍으로 저장된다.
        Date now = new Date();
        String secretKeyUse = "";
        if ("normal".equals(secretKeyType)) {
            secretKeyUse = secretKey;
        } else if ("test".equals(secretKeyType)) {
            secretKeyUse = secretKey1;
        }

        //Access Token
        return Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + jwtDto.getValidTimeRefresh())) // set Expire Time
                .signWith(SignatureAlgorithm.HS256, secretKeyUse)  // 사용할 암호화 알고리즘과
                // signature 에 들어갈 secret값 세팅
                .compact();
    }

    /**
     * 토큰의 Claim 디코딩
     *
     * @param secretKeyType
     * @param token
     * @return
     */
    public Claims getAllClaims(String secretKeyType, String token) {
        String secretKeyUse = "";
        if ("normal".equals(secretKeyType)) {
            secretKeyUse = secretKey;
        } else if ("test".equals(secretKeyType)) {
            secretKeyUse = secretKey1;
        }

        return Jwts.parser()
                .setSigningKey(secretKeyUse)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Claim 에서 ip 가져오기
     *
     * @param secretKeyType
     * @param token
     * @return
     */
    public String getIpFromToken(String secretKeyType, String token) {
        Map tokenMap = (Map) getAllClaims(secretKeyType, token).get("tokenMap");

        return (String) tokenMap.get("ip");
    }

    /**
     * Claim 에서 토큰만료시간 가져오기
     *
     * @param secretKeyType
     * @param token
     * @return
     */
    public int getExpirFromToken(String secretKeyType, String token) {
        return (int) getAllClaims(secretKeyType, token).get("exp");
    }

    /**
     * Claim 에서 토큰등록시간 가져오기
     *
     * @param secretKeyType
     * @param token
     * @return
     */
    public int getRegdateFromToken(String secretKeyType, String token) {
        return (int) getAllClaims(secretKeyType, token).get("iat");
    }
}