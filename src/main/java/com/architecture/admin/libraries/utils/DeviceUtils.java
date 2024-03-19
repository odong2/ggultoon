package com.architecture.admin.libraries.utils;

import org.apache.commons.lang.StringUtils;

public class DeviceUtils {

    public final static String ORIGIN = "origin";
    public final static String MOBILE = "m";
    public final static String TABLET = "mobile"; // 임시로 mobile 로 해둘 것. 추후 태블릿 이미지 생기면 변경
    public final static String PC = "pc";

    protected static final String[] MOBILE_LIST = {
            "mobileexplorer", "palmsource", "palmscape", "mobile", "phone",
            // Phones and Manufacturers
            "motorola", "nokia", "palm", "iphone", "ipod", "sony", "ericsson", "blackberry", "cocoon",
            "blazer", "lg", "amoi", "xda", "mda", "vario", "htc", "gxlaxy", "sharp", "sie-", "alcatel",
            "benq", "ipaq", "mot-", "playstation portable", "hiptop", "nec-", "panasonic", "philips",
            "sagem", "sanyo", "spv", "zte", "sendo", "pixel", "xperia", "oneplus", "moto", "redmi", "honor",
            "nokia", "zenfone", "oppo", "vivo", "realme", "axon", "aquos", "blade", "elite", "nexus", "blackberry"
            , "cat", "droid", "essential", "fire", "g power", "hydrogen", "icon", "jitterbug", "k4", "lucid"
            , "p8", "q6", "razer", "stylo", "thunderbolt", "u11", "v30", "wing", "xperia x", "yotaphone"
            , "z2 force", "axon 7", "blade v8", "c9 pro", "droid turbo", "e5 plus", "fierce 2", "g flex", "honor 9"
            , "idol 3", "j7 pro", "k8 plus", "lumia 950", "moto g", "nexus 5x", "oneplus 6t", "pixel 2", "q7 plus"
            , "r1 hd", "stylo 4", "turbo 2", "u12 plus", "v40 thinq", "x4", "y9 prime", "z3 play", "axon m"
            , "blade z max", "c7 pro", "desire 530", "moto x", "nexus 6p", "oppo f5", "pixel 3", "q stylus"
            , "r11s", "swift 2", "u ultra", "v50 thinq", "xperia xa", "y7 prime", "z force", "axon 10 pro"
            , "blade v7", "c5 pro", "droid mini", "essential ph-1", "f1s", "g6 plus", "honor 10", "idol 4", "j3 pro"
            , "k10 plus", "lumia 1520", "moto z", "nexus 6", "oneplus 5t", "p20 pro", "q6 plus", "r7 plus",
            // Operating Systems
            "SymbianOS", "symbian", "elaine", "palm", "series60", "windows ce",
            // Browsers
            "obigo", "netfront", "openwave", "operamini", "opera mini",
            // Other
            "digital paths", "avantgo", "xiino", "novarra", "vodafone", "docomo", "o2",
            // Fallback
            "mobile", "wireless", "j2me", "midp", "cldc", "up.link", "up.browser", "smartphone", "cellphone"
    };
    protected static final String[] TABLET_LIST = {
            "ipad", "galaxy tab", "galaxy book", "microsoft surface", "kindle fire", "pixel slate", "asus transformer pad",
            "asus transformer book", "asus zenpad", "asus memo pad", "lenovo yoga tab", "lenovo tab", "lenovo miix",
            "huawei mediapad", "huawei matepad", "huawei honor pad", "huawei matepad pro", "sony xperia tablet", "sony xperia z4 tablet",
            "sony xperia z3 tablet compact", "sony xperia z2 tablet", "toshiba encore", "toshiba excite", "acer iconia tab",
            "acer aspire switch", "acer chromebook tab", "dell venue", "dell latitude", "hp slate", "hp elitepad",
            "hp envy x2", "hp spectre x2", "panasonic toughpad", "panasonic let's note", "fujitsu stylistic",
            "fujitsu lifebook", "lg g pad", "alcatel onetouch pop", "alcat"
    };

    private DeviceUtils() {
    }

    /**
     * 모바일 체크
     *
     * @param device
     * @return
     */
    public static boolean isMobile(String device) {
        for (String mobile : MOBILE_LIST) {
            if (StringUtils.containsIgnoreCase(device, mobile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 태블릿 체크
     *
     * @param device
     * @return
     */
    public static boolean isTablet(String device) {
        for (String tablet : TABLET_LIST) {
            if (StringUtils.containsIgnoreCase(device, tablet)) {
                return true;
            }
        }
        return false;
    }


}
