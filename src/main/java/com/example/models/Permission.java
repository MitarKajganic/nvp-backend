package com.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

public enum Permission {
    can_create_users,
    can_read_users,
    can_update_users,
    can_delete_users,
    can_search_vacuum,
    can_start_vacuum,
    can_stop_vacuum,
    can_discharge_vacuum,
    can_add_vacuum,
    can_remove_vacuum
}
