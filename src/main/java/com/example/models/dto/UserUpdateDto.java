package com.example.models.dto;

import com.example.models.enums.Permission;
import lombok.Data;

import java.util.Set;

@Data
public class UserUpdateDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Set<Permission> permissions;
}
