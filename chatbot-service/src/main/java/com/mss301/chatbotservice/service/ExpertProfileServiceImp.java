package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.model.ExpertProfile;
import com.mss301.chatbotservice.repository.ExpertProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpertProfileServiceImp implements ExpertProfileService {

    @Autowired
    private ExpertProfileRepository expertProfileRepository;

    @Override
    public ExpertProfile save(ExpertProfile expertProfile) {
        return expertProfileRepository.save(expertProfile);
    }

    @Override
    public List<ExpertProfile> findAll() {
        return expertProfileRepository.findAll();
    }
}
