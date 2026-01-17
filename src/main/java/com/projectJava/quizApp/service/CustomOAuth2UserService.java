package com.projectJava.quizApp.service;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private UserRepo customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = new DefaultOAuth2UserService().loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String providerId = oauthUser.getAttribute("sub"); // unique ID from provider
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        Customer customer = customerRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setUsername(name);
                    newCustomer.setEmail(email);
                    newCustomer.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newCustomer.setRole("USER");
                    newCustomer.setProvider(provider);
                    newCustomer.setProviderId(providerId);
                    return customerRepository.save(newCustomer);
                });

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + customer.getRole())),
                oauthUser.getAttributes(),
                "email");
    }
}
