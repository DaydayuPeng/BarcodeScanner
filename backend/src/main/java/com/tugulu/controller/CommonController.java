package com.tugulu.controller;

import com.tugulu.common.ApiResponse;
import com.tugulu.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    private static final DateTimeFormatter LOCAL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${tugulu.upload.dir:uploads}")
    private String uploadDir;

    @Value("${tugulu.upload.base-url:/uploads/}")
    private String baseUrl;

    @GetMapping("/current-time")
    public ApiResponse<Map<String, String>> currentTime() {
        ZonedDateTime utc = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime beijing = utc.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
        LocalDateTime local = LocalDateTime.now();
        Map<String, String> data = new HashMap<>();
        data.put("beijingTime", beijing.format(LOCAL_FMT));
        data.put("localTime", local.format(LOCAL_FMT));
        data.put("utcTime", utc.format(DateTimeFormatter.ISO_INSTANT));
        return ApiResponse.ok(data);
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx >= 0) {
            ext = original.substring(idx);
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());
        Map<String, String> data = new HashMap<>();
        data.put("url", baseUrl + filename);
        return ApiResponse.ok("上传成功", data);
    }
}
