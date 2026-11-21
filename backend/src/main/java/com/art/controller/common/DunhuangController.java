package com.art.controller.common;

import com.art.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 敦煌舞数字展馆：代理Python评分服务的敦煌参考数据端点（只读）
 * 提供8大敦煌舞动作类别元数据与MediaPipe 33关键点参考骨架序列
  * @author liukang&&heyan
 */
@Slf4j
@RestController
@RequestMapping("/api/dunhuang")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DunhuangController {

    @Value("${dance.api.url:http://127.0.0.1:8000}")
    private String danceApiUrl;

    private RestTemplate buildRestTemplate(int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    private RestTemplate buildRestTemplate() {
        // 参考骨架序列payload较大（150帧×33关键点），读取超时放宽到60s
        return buildRestTemplate(60000);
    }

    /** 8大敦煌舞动作类别元数据 + 标志性关键姿态特征 */
    @GetMapping("/categories")
    public Result<Map<String, Object>> categories() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = buildRestTemplate()
                    .getForObject(danceApiUrl + "/api/dunhuang/categories", Map.class);
            return Result.success(resp);
        } catch (Exception e) {
            log.warn("敦煌类别数据代理失败: {}", e.getMessage());
            return Result.error("敦煌参考数据服务暂不可用，请稍后重试");
        }
    }

    /** 指定类别的参考骨架序列（150帧 × 33关键点，640×480像素坐标） */
    @GetMapping("/reference/{category}")
    public Result<Map<String, Object>> reference(@PathVariable String category) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = buildRestTemplate()
                    .getForObject(danceApiUrl + "/api/dunhuang/reference/" + category, Map.class);
            return Result.success(resp);
        } catch (Exception e) {
            log.warn("敦煌参考骨架代理失败({}): {}", category, e.getMessage());
            return Result.error("敦煌参考数据服务暂不可用，请稍后重试");
        }
    }

    /**
     * 关键姿态挑战：转发学员照片到Python服务做MediaPipe姿态识别与四姿态匹配评分。
     * 首次调用需冷启动MediaPipe模型，读取超时放宽到120s；照片由Python侧即时处理即时删除。
     */
    @PostMapping("/pose-challenge")
    public Result<Map<String, Object>> poseChallenge(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请先拍摄或选择挑战照片");
        }
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "challenge.jpg";
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = buildRestTemplate(120000)
                    .postForObject(danceApiUrl + "/api/dunhuang/pose-challenge",
                            new HttpEntity<>(body, headers), Map.class);
            return Result.success(resp);
        } catch (Exception e) {
            log.warn("关键姿态挑战代理失败: {}", e.getMessage());
            return Result.error("姿态识别服务暂不可用，请稍后重试");
        }
    }
}
