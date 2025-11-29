package com.newwork.employee.service;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.dto.ProfileUpdateDTO;

import java.util.UUID;

/**
 * Contract for profile operations with permission-aware field handling.
 */
public interface ProfileService {

    ProfileDTO getProfile(UUID viewerId, UUID profileUserId);

    ProfileDTO updateProfile(UUID viewerId, UUID profileUserId, ProfileUpdateDTO updateDTO);
}
