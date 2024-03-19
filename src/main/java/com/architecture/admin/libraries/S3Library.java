package com.architecture.admin.libraries;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.squareup.pollexor.Thumbor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/*****************************************************
 * S3 라이브러리
 ****************************************************/
@Service
@RequiredArgsConstructor
@Log4j2
public class S3Library {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cf.imgurl}")
    private String cfImgURL;

    // 이미지 FULL URL (도메인 포함)
    private String fullUrl;
    private final AmazonS3 amazonS3;

    /** thumbor domain */
    @Value("${cloud.aws.cf.url}")
    private String thumborDomain;

    /** thumbor key */
    @Value("${thumbor.key}")
    private String thumborKey;

    /**
     * Thumbor full url로 이미지 세팅
     * @param map
     * @return
     */
    public String getThumborFullUrl(Map<String, Object> map) {

        // map data
        String fileUrl = (String) map.get("fileUrl"); // 이미지 url
        Integer width = (Integer) map.get("width");   // 이미지 가로 사이즈
        Integer height = (Integer) map.get("height"); // 이미지 세로 사이즈

        // return value (기본값 : 원본 이미지 경로)
        String thumborUrl = getUploadedFullUrl(fileUrl);

        try {

            Thumbor thumbor = Thumbor.create(thumborDomain, thumborKey);

            // 가로 & 세로 사이즈 값이 있을 때만 리사이징
            if (width != null && height != null) {
                thumborUrl = thumbor.buildImage(thumborUrl).resize(width, height).toUrl();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return thumborUrl;
    }


    /*
     * cf domain 포함하여 full url 리턴
     * @param string fileUrl  기 db 저장된 url (예시: /store/26/55f24d86-3ef3-4dc1-a4d1-08afd03d4674.jpg)
     */
    public String getUploadedFullUrl(String fileUrl) {
        try {
            return "https://" + cfImgURL + fileUrl;
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TO get uploaded file(" + fileUrl + ") full url is error.");
        }
    }

    public List<String> uploadFile(List<MultipartFile> multipartFile) {
        List<String> fileNameList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileNameList에 추가
        multipartFile.forEach(file -> {
            String fileName = createFileName(file.getOriginalFilename());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            try(InputStream inputStream = file.getInputStream()) {
                amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));
            } catch(IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload");
            }

            // 도메인 없이 설정
            // String fileUrl = "https://" + bucket.toString() + "/" + fileName;
            String fileUrl = "/" + fileName;

            //fileNameList.add(fileName);
            fileNameList.add(fileUrl);
        });

        return fileNameList;
    }

    public List<String> uploadFile(List<MultipartFile> multipartFile, String dirName) {
        List<String> fileNameList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileNameList에 추가
        multipartFile.forEach(file -> {
            String fileName = dirName + "/" + createFileName(file.getOriginalFilename());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            try(InputStream inputStream = file.getInputStream()) {
                amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));
            } catch(IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload.");
            }

            String fileUrl = "https://" + bucket.toString() + "/" + fileName;

            //fileNameList.add(fileName);
            fileNameList.add(fileUrl);
        });

        return fileNameList;
    }

    // 파일 정보 list map 리턴 추가
    public List<HashMap<String,Object>> uploadFileNew(List<MultipartFile> multipartFile) {
        List<HashMap<String,Object>> fileUploadList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileUploadList에 추가
        multipartFile.forEach(file -> {

            if (!file.isEmpty()) {// 있을 경우
                HashMap<String, Object> fileInfoList = new HashMap<>();

                String fileName = createFileName(file.getOriginalFilename());
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(file.getSize());
                objectMetadata.setContentType(file.getContentType());

                try(InputStream inputStream = file.getInputStream()) {
                    amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));
                } catch(IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload.");
                }

                fileInfoList.put("fileUrl", "/" + fileName);// url 설정 정보(도메인 없이 설정)
                fileInfoList.put("newFileName", fileName);// 업로드 시 변경된 파일 이름
                fileInfoList.put("orgFileName", file.getOriginalFilename());// 파일 원 이름
                fileInfoList.put("fileSize", file.getSize());// 파일 사이즈
                fileInfoList.put("fileExtension", file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));// 파일 확장자 => getFileExtension() 메소드 미사용은 exception 때문에,,
                fileInfoList.put("fileContentType", file.getContentType());// 파일 ContentType

                fileUploadList.add(fileInfoList);// 리턴할 정보에 입력처리
            }
        });

        return fileUploadList;
    }

    // 파일 정보 list map 리턴 추가
    public List<HashMap<String,Object>> uploadFileNew(List<MultipartFile> multipartFile, String dirName) {
        List<HashMap<String,Object>> fileUploadList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileUploadList에 추가
        multipartFile.forEach(file -> {

            if (!file.isEmpty()) {// 있을 경우
                HashMap<String, Object> fileInfoList = new HashMap<>();

                String fileName = dirName + "/" + createFileName(file.getOriginalFilename());
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(file.getSize());
                objectMetadata.setContentType(file.getContentType());

                try(InputStream inputStream = file.getInputStream()) {
                    amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));
                } catch(IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload.");
                }

                fileInfoList.put("fileUrl", "/" + fileName);// url 설정 정보(도메인 없이 설정)
                fileInfoList.put("newFileName", fileName);// 업로드 시 변경된 파일 이름
                fileInfoList.put("orgFileName", file.getOriginalFilename());// 파일 원 이름
                fileInfoList.put("fileSize", file.getSize());// 파일 사이즈
                fileInfoList.put("fileExtension", file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));// 파일 확장자 => getFileExtension() 메소드 미사용은 exception 때문에,,
                fileInfoList.put("fileContentType", file.getContentType());// 파일 ContentType

                fileUploadList.add(fileInfoList);// 리턴할 정보에 입력처리
            }
        });

        return fileUploadList;
    }

    public void deleteFile(String fileName) {
        boolean isExistObject = amazonS3.doesObjectExist(bucket, fileName);
        if (Boolean.TRUE.equals(isExistObject)) {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
        }
    }

    /*
     * 동일 버킷의 파일 복사
     * !! 복사 완료 후 복사대상 파일 삭제처리(tmp 파일임으로) - data/tmpUpload(cloud.aws.s3.tmpFolder) 폴더 내 파일은 수정날짜 기준 24시간 이후 자동 삭제 추가 설정 예정
     * @param string sourceFileName  S3에 저장되어 있는 복사대상 파일 이름 (예시:cce2a417-b4fa-4706-ad91-f956134f895b.jpg)
     * @param string sourcePath  S3에 저장되어 있는 복사대상 파일의 경로 (S3도메인 및 파일 이름을 제외한 파일의 경로, 예시:data/tmpUpload) => '/'가 앞뒤에 없음 주의!!
     * @param string destinationPath  S3에 복사할 S3 파일의 경로 (S3도메인을 제외한 파일의 경로, 예시: store/숫자(idx)) => '/'가 앞뒤에 없음 주의!!
     */
    public void copyFile(String sourceFileName, String sourcePath, String destinationPath) {

        String sourceKey = sourcePath + "/" + sourceFileName;// 복사대상 파일의 S3 KEY
        String destinationKey = destinationPath + "/" + sourceFileName;// 복사할 S3 KEY

        try {
            boolean isExistObject = amazonS3.doesObjectExist(bucket, sourceKey);// 복사대상 파일 존재여부 체크
            if (Boolean.TRUE.equals(isExistObject)) {
                //Copy 객체 생성
                CopyObjectRequest copyObjRequest = new CopyObjectRequest(
                        bucket,
                        sourceKey,
                        bucket,
                        destinationKey
                );
                //Copy
                amazonS3.copyObject(copyObjRequest);

                boolean isExistDestinationObject = amazonS3.doesObjectExist(bucket, destinationKey);// 복사 완료한 파일 존재여부 체크
                if (Boolean.TRUE.equals(isExistDestinationObject)) {
                    //deleteFile(sourceKey);// 복사대상 파일 삭제처리
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fail File Copy.");
        }
    }

    /*
     * 기 저장된 url에서 업로드 된 파일이름만 추출하여 리턴
     * @param string fileUrl  기 db 저장된 url 또는 fullUrl (예시: /data/tmpUpload/55f24d86-3ef3-4dc1-a4d1-08afd03d4674.jpg 또는 //dev-imgs.devlabs.co.kr/data/tmpUpload/90a19f7b-ca87-422e-a31e-b65b28d8f5c7.jpg)
     * @param string path  기 db 저장된 path (예시: store/26) => '/'가 앞뒤에 없음 주의!!
     */
    public String getUploadedFileName(String fileUrl, String path) {
        try {
            String tmpFileUrl = fileUrl.replace("//" + cfImgURL, "");// 도메인 제거
            return tmpFileUrl.replace("/" + path + "/", "");
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TO get uploaded file(" + fileUrl + ") name is wrong url.");
        }
    }


    private String createFileName(String fileName) { // 먼저 파일 업로드 시, 파일명을 난수화하기 위해 random으로 돌립니다.
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }

    private String getFileExtension(String fileName) { // file 형식이 잘못된 경우를 확인하기 위해 만들어진 로직이며, 파일 타입과 상관없이 업로드할 수 있게 하기 위해 .의 존재 유무만 판단하였습니다.
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file(" + fileName + ") is malformed.");
        }
    }
}
