package com.org.dev.cloud.useraccount.controller;

import com.org.dev.cloud.useraccount.dto.UserProfileResponse;
import com.org.dev.cloud.useraccount.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    Mono<String> test(){
      return userRepository.existsByExternalId("testId")
              .flatMap(exists -> {
                  if (exists) {
                      return Mono.error(new RuntimeException("User already exists"));
                  }
                  return Mono.empty();
              });
    }
}
