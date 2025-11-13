package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.model.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertProfileService {
    ExpertProfile save(ExpertProfile expertProfile);
    List<ExpertProfile> findAll();
}
