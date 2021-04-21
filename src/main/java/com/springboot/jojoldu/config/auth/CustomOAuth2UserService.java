package com.springboot.jojoldu.config.auth;

import com.springboot.jojoldu.config.auth.dto.OAuthAttributes;
import com.springboot.jojoldu.config.auth.dto.SessionUser;
import com.springboot.jojoldu.domain.user.User;
import com.springboot.jojoldu.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest,OAuth2User> {
    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2UserService<OAuth2UserRequest,OAuth2User> delegate=new DefaultOAuth2UserService();
        OAuth2User oAuth2User=delegate.loadUser(userRequest);
        
        // 현재 로그인 진행 중인 서비스를 구분(google,naver,kakao)
        String registrationId=userRequest.getClientRegistration().getRegistrationId();
        // OAuth2 로그인 진행시 필드값, PK
        String userNameAttributeName=userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        
        // OAuth2User의 attribute를 담을 클래스
        OAuthAttributes attributes=OAuthAttributes.of(registrationId,userNameAttributeName,
                oAuth2User.getAttributes());
        User user = saveOrUpdate(attributes);
        
        // SessionUser를 등록(세션에 사용자 정보를 저장)
        httpSession.setAttribute("user",new SessionUser(user));

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes){
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(),attributes.getPicture()))
                .orElse(attributes.toEntity());
        return userRepository.save(user);
    }
}
