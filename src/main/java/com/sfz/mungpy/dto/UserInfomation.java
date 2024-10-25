package com.sfz.mungpy.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UserInfomation {
    private List<String> personality;
    private MultipartFile image;
}
