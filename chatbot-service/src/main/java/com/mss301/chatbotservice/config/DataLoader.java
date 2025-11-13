package com.mss301.chatbotservice.config;

import com.mss301.chatbotservice.model.ExpertProfile;
import com.mss301.chatbotservice.service.ExpertProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ExpertProfileService expertProfileService;

    @Override
    public void run(String... args) throws Exception {
        if (expertProfileService.findAll().isEmpty()) {
            initExpertProfile();
        }
    }

    private void initExpertProfile(){
        ExpertProfile deepseek = new ExpertProfile();
        deepseek.setName("Deepseek v3.1");
        deepseek.setCode("deepseek-v3.1");
        deepseek.setDescription("This is the Expert Profile");
        deepseek.setActive(true);
        deepseek.setPromptConfig("deepseek/deepseek-chat-v3.1:free");
        expertProfileService.save(deepseek);

        ExpertProfile gemma3n = new ExpertProfile();
        gemma3n.setName("Gemma 3n");
        gemma3n.setCode("gemma-3n-e2b");
        gemma3n.setDescription("This is the Expert Profile");
        gemma3n.setActive(true);
        gemma3n.setPromptConfig("google/gemma-3n-e2b-it:free");
        expertProfileService.save(gemma3n);

        ExpertProfile gemma12b = new ExpertProfile();
        gemma12b.setName("Gemma 3");
        gemma12b.setCode("gemma-3-12b");
        gemma12b.setDescription("This is the Expert Profile");
        gemma12b.setActive(true);
        gemma12b.setPromptConfig("google/gemma-3-12b-it:free");
        expertProfileService.save(gemma12b);
    }
}
