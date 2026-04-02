package com.vieterp.otb.auth.dto;

import lombok.*;

@Data
public class UpdateProfileRequest {
    private String name;
    private String phone;
    private String department;
}
