package com.cqwu.cqwu_appointment_system.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.glxn.qrgen.javase.QRCode;
import org.springframework.util.StringUtils;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * @description 二维码生成工具类
 */
@Slf4j
public final class QrCodeUtils {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private QrCodeUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 根据指定内容生成二维码，并返回 Base64 编码的 Data URI 字符串。
     * 使用默认尺寸 (250x250)。
     *
     * @param content 要编码到二维码中的字符串内容
     * @return Base64 编码的 Data URI (例如 "data:image/png;base64,iVBORw0KGgo...")
     * @throws IllegalArgumentException 如果 content 为空或空白字符串
     * @throws RuntimeException         如果二维码生成过程中发生错误
     */
    public static String generateQrCodeBase64(String content) {
        return generateQrCodeBase64(content, Constants.QrCode.DEFAULT_WIDTH, Constants.QrCode.DEFAULT_HEIGHT);
    }

    /**
     * 根据指定内容和尺寸生成二维码，并返回 Base64 编码的 Data URI 字符串。
     *
     * @param content 要编码到二维码中的字符串内容
     * @param width   二维码图片的宽度（像素）
     * @param height  二维码图片的高度（像素）
     * @return Base64 编码的 Data URI (例如 "data:image/png;base64,iVBORw0KGgo...")
     * @throws IllegalArgumentException 如果 content 为空或空白字符串
     * @throws RuntimeException         如果二维码生成过程中发生错误
     */
    public static String generateQrCodeBase64(String content, int width, int height) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("QR code content cannot be empty or null.");
        }

        log.debug("开始为内容生成二维码: [{}]", content);

        try (ByteArrayOutputStream stream = QRCode.from(content).withSize(width, height).stream()) {

            byte[] qrCodeBytes = stream.toByteArray();
            if (qrCodeBytes.length == 0) {
                throw new RuntimeException("Generated QR code byte stream is empty.");
            }

            String base64String = Base64.getEncoder().encodeToString(qrCodeBytes);

            String result = Constants.BASE64_URI_PREFIX + base64String;
            log.debug("成功生成二维码, Base64 Data URI 长度: {}", result.length());

            return result;

        } catch (Exception e) {
            log.error("生成二维码时发生严重错误, content: {}", content, e);
            
            throw new RuntimeException("Failed to generate QR code for content: " + content, e);
        }
    }
}