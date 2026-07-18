package com.tugulu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tugulu.common.BusinessException;
import com.tugulu.common.UserContext;
import com.tugulu.dto.CmsUpdateRequest;
import com.tugulu.entity.CmsContent;
import com.tugulu.mapper.CmsContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CmsService {

    private static final Set<String> TYPES = Set.of("company", "service", "product");

    private final CmsContentMapper cmsContentMapper;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getByType(String type) {
        validateType(type);
        CmsContent content = cmsContentMapper.selectOne(new LambdaQueryWrapper<CmsContent>()
                .eq(CmsContent::getType, type)
                .orderByDesc(CmsContent::getSortOrder)
                .last("LIMIT 1"));
        if (content == null) {
            throw new BusinessException("内容不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", content.getId());
        data.put("titleEn", content.getTitleEn());
        data.put("contentEn", content.getContentEn());
        data.put("contentZh", content.getContentZh());
        data.put("images", parseImages(content.getImages()));
        return data;
    }

    public void update(String type, CmsUpdateRequest request) {
        validateType(type);
        CmsContent content = cmsContentMapper.selectOne(new LambdaQueryWrapper<CmsContent>()
                .eq(CmsContent::getType, type)
                .last("LIMIT 1"));
        if (content == null) {
            content = new CmsContent();
            content.setType(type);
            content.setSortOrder(100);
        }
        content.setTitleEn(request.getTitleEn());
        content.setContentEn(request.getContentEn());
        content.setContentZh(request.getContentZh());
        try {
            content.setImages(objectMapper.writeValueAsString(
                    request.getImages() == null ? Collections.emptyList() : request.getImages()));
        } catch (Exception e) {
            throw new BusinessException("图片列表格式错误");
        }
        content.setUpdateBy(UserContext.getRealName());
        if (content.getId() == null) {
            cmsContentMapper.insert(content);
        } else {
            cmsContentMapper.updateById(content);
        }
    }

    private void validateType(String type) {
        if (!TYPES.contains(type)) {
            throw new BusinessException("无效的内容类型");
        }
    }

    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
