package com.websystique.springboot.repository;

import com.websystique.springboot.model.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, Long> {
    Mono<Boolean> existsByName(String name);
}
