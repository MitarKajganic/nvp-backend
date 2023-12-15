package com.example.models.dto;

import com.example.models.Permission;
import lombok.Data;

import javax.persistence.*;
import java.util.Set;

@Data
public class UserUpdateDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Set<Permission> permissions;
}
