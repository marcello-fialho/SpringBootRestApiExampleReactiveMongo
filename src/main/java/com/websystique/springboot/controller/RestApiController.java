package com.websystique.springboot.controller;

import java.util.ArrayList;
import java.util.List;

import com.websystique.springboot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.websystique.springboot.model.User;
import com.websystique.springboot.util.CustomErrorType;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class RestApiController {

    private static final Logger logger = LoggerFactory.getLogger(RestApiController.class);
    private UserRepository userRepository; //Service which will do all data retrieval/manipulation work

    @Autowired
    public RestApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // -------------------Retrieve All Users---------------------------------------------

    @RequestMapping(value = "/user/", method = RequestMethod.GET)
    public Mono<ResponseEntity<List<User>>> listAllUsers() {
        List<User> userList = new ArrayList<>();
        return userRepository.findAll().reduce(userList, (acc, curr) -> {
            acc.add(curr);
            return acc;
        }).map(uList -> uList.size() > 0 ? new ResponseEntity<>(uList, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    // -------------------Retrieve Single User------------------------------------------

    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    public Mono<ResponseEntity<Object>> getUser(@PathVariable("id") long id) {
        logger.info("Fetching User with id {}", id);
        return userRepository.findById(id).map(user -> new ResponseEntity<Object>(user, HttpStatus.OK))
                .switchIfEmpty(Mono.just(new ResponseEntity<Object>(new CustomErrorType("User with id " + id + " not found"), HttpStatus.NOT_FOUND))
                        .doOnNext(__ -> logger.error("User with id " + id + " not found")));
    }

    // -------------------Create a User-------------------------------------------

    @RequestMapping(value = "/user/", method = RequestMethod.POST)
    public Mono<ResponseEntity<?>> createUser(@RequestBody User user, UriComponentsBuilder ucBuilder) {
        logger.info("Creating User : {}", user);
        return userRepository.existsByName(user.getName()).flatMap(exists -> {
            if (exists) {
                logger.error("Unable to create. A User with name " + user.getName() + " already exist.");
                return Mono.just(new ResponseEntity<>(new CustomErrorType("Unable to create. A User with name " +
                        user.getName() + " already exist."), HttpStatus.CONFLICT));
            } else {
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(ucBuilder.path("/api/user/{id}").buildAndExpand(user.getId()).toUri());
                return userRepository.save(user).then(Mono.just(new ResponseEntity<String>(headers, HttpStatus.CREATED)));
            }
        });
    }

    // ------------------- Update a User ------------------------------------------------

    @RequestMapping(value = "/user/{id}", method = RequestMethod.PUT)
    public Mono<ResponseEntity<Object>> updateUser(@PathVariable("id") long id, @RequestBody User user) {
        logger.info("Updating User with id {}", id);
        return userRepository.findById(id).flatMap(currentUser -> {
            user.setId(currentUser.getId());
            return userRepository.save(user).then(Mono.just(new ResponseEntity<Object>(user, HttpStatus.OK)));
        }).switchIfEmpty(Mono.just(new ResponseEntity<Object>(new CustomErrorType("Unable to update. User with id " + id + " not found."),
                HttpStatus.NOT_FOUND)).doOnNext(__ -> logger.error("Unable to update. User with id " + id + " not found.")));
    }
    // ------------------- Delete a User-----------------------------------------

    @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
    public Mono<ResponseEntity<Object>> deleteUser(@PathVariable("id") long id) {
        logger.info("Fetching & Deleting User with id {}", id);
        return userRepository.findById(id).flatMap(user -> userRepository.delete(user).then(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)))
                .switchIfEmpty(Mono.just(new ResponseEntity<>(new CustomErrorType("Unable to delete. User with id " + id + " not found."), HttpStatus.NOT_FOUND)))
                .doOnNext(__ -> logger.error("Unable to delete. User with id " + id + " not found.")));

    }

    // ------------------- Delete All Users-----------------------------

    @RequestMapping(value = "/user/", method = RequestMethod.DELETE)
    public Mono<ResponseEntity<User>> deleteAllUsers() {
        logger.info("Deleting All Users");
        return userRepository.deleteAll().then(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)));
    }

}
