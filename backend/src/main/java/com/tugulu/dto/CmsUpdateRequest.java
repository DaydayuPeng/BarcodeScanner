package com.tugulu.dto;

import lombok.Data;

import java.util.List;

@Data
public class CmsUpdateRequest {
    private String titleEn;
    private String contentEn;
    private String contentZh;
    private List<String> images;
}
