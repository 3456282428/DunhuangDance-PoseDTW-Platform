package com.art.common;

import java.util.regex.Pattern;

/**
 * 手机号统一校验：全系统手机号固定为 11 位中国大陆手机号
 *
 * @author liukang&&heyan
 */
public final class MobileValidator {

    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    private MobileValidator() {
    }

    public static boolean isValid(String phone) {
        return phone == null || phone.isBlank() || MOBILE.matcher(phone.trim()).matches();
    }

    /** 非空时必须为 11 位有效手机号，否则抛业务异常 */
    public static void require(String phone, String label) {
        if (phone != null && !phone.isBlank() && !MOBILE.matcher(phone.trim()).matches()) {
            throw new BusinessException(label + "必须为11位有效手机号");
        }
    }

    /** 必填场景：不能为空且必须为 11 位有效手机号 */
    public static void requireNonNull(String phone, String label) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(label + "不能为空");
        }
        if (!MOBILE.matcher(phone.trim()).matches()) {
            throw new BusinessException(label + "必须为11位有效手机号");
        }
    }
}
