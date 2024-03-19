package com.architecture.admin.libraries.utils.pg;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

public class HttpClientUtil {
	private Logger logger = LoggerFactory.getLogger("trans");
	
	public String sendApi(String targetUrl, Map<String, Object> param, int connTimeout, int readTimeout) {
		
		//파라미터 JSON객체로 변환
		JSONObject jsonObj = JSONObject.fromObject(param);
		
		//로그표시용 주문번호 얻기
		JSONObject tmp = jsonObj.has("params") ? jsonObj.getJSONObject("params") : null;
		String trdNo = (tmp == null) ? "" : tmp.getString("mchtTrdNo");
		
		logger.info("["+trdNo+"]=========================START SEND API=========================");
		
		HttpsURLConnection httpsURLConnection = null;
		
		String sendData = "";
		String resData = "";
		
		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new javax.net.ssl.X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
				public X509Certificate[] getAcceptedIssuers() {return null;}
				
			}};
			
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			
			URL url = new URL( targetUrl );
			
			logger.info("["+trdNo+"][API Send URL]" + url);
			logger.info("["+trdNo+"][URL Protocol]" + url.getProtocol() + " [Connect Timeout]"+connTimeout +" [Read Timeout]" + readTimeout);
			
			httpsURLConnection = (HttpsURLConnection)url.openConnection();
			httpsURLConnection.setDoInput(true);
			httpsURLConnection.setDoOutput(true);
			httpsURLConnection.setRequestProperty("Content-Type", "application/json");
			httpsURLConnection.setRequestProperty("charset", "UTF-8");
			httpsURLConnection.setConnectTimeout(connTimeout);
			httpsURLConnection.setReadTimeout(readTimeout);
			
			//JSON스트링으로 변환
			sendData = jsonObj.toString();
			
			//보낼 데이터
			logger.info("["+trdNo+"][Send Data]" + sendData);
			
			OutputStream os = null;
			os = httpsURLConnection.getOutputStream();
			
			os.write(sendData.getBytes("UTF-8"));
			os.flush();
			os.close();
			
			logger.info("["+trdNo+"][Response Code]" + httpsURLConnection.getResponseCode());
			if(httpsURLConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
				DataInputStream in = new DataInputStream(httpsURLConnection.getInputStream());
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				
				byte[] buf = new byte[1024];
				
				while(true) {
					int n = in.read(buf);
					if( n== -1) break;
					bout.write(buf, 0, n);
				}
				bout.flush();
				
				byte[] resMessage = bout.toByteArray();
				
				in.close();
				bout.close();
				
				resData = new String(resMessage, "UTF-8");
				
				logger.info("["+trdNo+"][Response Data]" + resData);
				logger.info("["+trdNo+"][Response Data] byte length : " + resData.getBytes().length);
				
			}else {
				logger.error("["+trdNo+"][Connect Error]" + httpsURLConnection.getResponseMessage());
				
				Map<String,String> params = new HashMap<String, String>();
				params.put("outStatCd", "0031");
				params.put("outRsltCd", "9999");
				params.put("outRsltMsg", "[Connect Error]" + httpsURLConnection.getResponseMessage());
				
				Map<String,String> data = new HashMap<String, String>();
				
				Map<String,Object> expMap = new HashMap<String, Object>();
				expMap.put("params", params);
				expMap.put("data", data);
				
				JSONObject expJson = JSONObject.fromObject(expMap);
				resData = expJson.toString();
			}
			
			httpsURLConnection.disconnect();
		}catch (Exception e) {
			logger.error("["+trdNo+"][HTTP Connect Error]" + e.toString());
			
			Map<String,String> params = new HashMap<String, String>();
			params.put("outStatCd", "0031");
			params.put("outRsltCd", "9999");
			params.put("outRsltMsg", "[HTTP Connect Error]" + e.toString());
			
			Map<String,String> data = new HashMap<String, String>();
			
			Map<String,Object> expMap = new HashMap<String, Object>();
			expMap.put("params", params);
			expMap.put("data", data);
			
			JSONObject expJson = JSONObject.fromObject(expMap);
			resData = expJson.toString();
		}finally {
			if(httpsURLConnection != null)
				httpsURLConnection.disconnect();
		}
		
		logger.info("["+trdNo+"]=========================END SEND API=========================");
		
		return resData;
	}
}
