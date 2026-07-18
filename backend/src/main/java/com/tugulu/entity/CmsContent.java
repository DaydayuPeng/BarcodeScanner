package com.tugulu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cms_content")
public class CmsContent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String titleEn;
    private String contentEn;
    private String contentZh;
    private String images;
    private Integer sortOrder;
    private String updateBy;
    private LocalDateTime updateTime;
}
