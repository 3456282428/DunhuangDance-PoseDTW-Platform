package com.art.controller;

import com.art.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件上传控制器（作业图片/视频/PDF）
  * @author liukang&&heyan
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileController {

    /** 视频压缩阈值：超过该大小的视频用ffmpeg转码（4K高码率原片浏览器解码卡顿、且占用数倍存储） */
    private static final long VIDEO_COMPRESS_THRESHOLD = 100L * 1024 * 1024;
    private static final List<String> VIDEO_EXTS = List.of(".mp4", ".mov", ".webm", ".avi", ".mkv", ".wmv", ".m4v");

    @PostMapping
    public Result<List<Map<String, String>>> upload(@RequestParam("files") MultipartFile[] files) {
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
            try {
                File dest = new File(dir, storedName);
                file.transferTo(dest);
                if (isVideo(ext) && dest.length() > VIDEO_COMPRESS_THRESHOLD) {
                    compressVideo(dest);
                }
                Map<String, String> item = new HashMap<>();
                item.put("url", "/uploads/" + storedName);
                item.put("name", original != null ? original : storedName);
                result.add(item);
            } catch (Exception e) {
                throw new RuntimeException("文件保存失败", e);
            }
        }
        return Result.success(result);
    }

    private boolean isVideo(String ext) {
        return ext != null && VIDEO_EXTS.contains(ext.toLowerCase());
    }

    /**
     * 压缩超大视频：高度≤720p（不放大）、30fps、H.264 CRF23、音频AAC 128k，
     * 并加 faststart 把 moov 移到文件头便于边下边播。
     * 压缩失败（如未安装ffmpeg）不影响上传结果，保留原文件。
     */
    private void compressVideo(File video) {
        File tmp = new File(video.getParentFile(), video.getName() + ".tmp.mp4");
        long srcBytes = video.length();
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", video.getAbsolutePath(),
                    "-vf", "scale=-2:min(ih\\,720)", "-r", "30",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
                    "-c:a", "aac", "-b:a", "128k",
                    "-movflags", "+faststart",
                    tmp.getAbsolutePath());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            p = pb.start();
            if (!p.waitFor(15, TimeUnit.MINUTES) || p.exitValue() != 0 || tmp.length() <= 0) {
                throw new IllegalStateException("ffmpeg 转码未成功");
            }
            if (!video.delete() || !tmp.renameTo(video)) {
                throw new IllegalStateException("替换原文件失败");
            }
            log.info("大视频已压缩: {} ({}MB -> {}MB)", video.getName(),
                    srcBytes / 1048576, video.length() / 1048576);
        } catch (Exception e) {
            log.warn("视频压缩跳过, 保留原文件: {} 原因: {}", video.getName(), e.getMessage());
        } finally {
            if (p != null && p.isAlive()) {
                p.destroyForcibly();
            }
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }
}
