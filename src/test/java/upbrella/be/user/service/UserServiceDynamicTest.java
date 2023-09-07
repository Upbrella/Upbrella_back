package upbrella.be.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import upbrella.be.user.dto.request.JoinRequest;
import upbrella.be.user.dto.request.KakaoAccount;
import upbrella.be.user.dto.response.KakaoLoginResponse;
import upbrella.be.user.dto.response.SessionUser;
import upbrella.be.user.exception.ExistingMemberException;
import upbrella.be.user.entity.User;
import upbrella.be.user.exception.NonExistingMemberException;
import upbrella.be.user.repository.UserRepository;
import upbrella.be.util.AesEncryptor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceDynamicTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Transactional
    @TestFactory
    @DisplayName("사용자는 회원 가입과 로그인을 할 수 있다.")
    Collection<DynamicTest> joinTest() {
        // given
        User user = User.builder()
                .id(1L)
                .socialId(23132L)
                .accountNumber(AesEncryptor.encrypt("110-421-674103"))
                .bank(AesEncryptor.encrypt("신한"))
                .email("email@email.com")
                .name("홍길동")
                .phoneNumber("010-2084-3478")
                .adminStatus(false)
                .build();

        JoinRequest joinRequest = JoinRequest.builder()
                .name("홍길동")
                .bank("신한")
                .accountNumber("110-421-674103")
                .phoneNumber("010-2084-3478")
                .build();

        KakaoLoginResponse kakaoUser = KakaoLoginResponse.builder()
                .id(23132L)
                .kakaoAccount(
                        KakaoAccount.builder()
                                .email("email@email.com")
                                .build())
                .build();

        return List.of(
                DynamicTest.dynamicTest("새로 가입한 유저는 DB에 저장된다.", () -> {
                    SessionUser joined = userService.join(kakaoUser, joinRequest);
                    Optional<User> foundUser = userRepository.findById(joined.getId());

                    assertAll(() -> assertTrue(foundUser.isPresent()),
                            () -> assertEquals(user.getName(), foundUser.get().getName()),
                            () -> assertEquals(user.getPhoneNumber(), foundUser.get().getPhoneNumber()),
                            () -> assertEquals(user.isAdminStatus(), foundUser.get().isAdminStatus()),
                            () -> assertEquals(user.getSocialId(), foundUser.get().getSocialId()),
                            () -> assertEquals(user.getAccountNumber(), foundUser.get().getAccountNumber()),
                            () -> assertEquals(user.getBank(), foundUser.get().getBank())
                    );
                }),
                DynamicTest.dynamicTest("이미 가입된 유저는 예외가 발생된다.", () -> {
                    assertThatThrownBy(() -> userService.join(kakaoUser, joinRequest))
                            .isInstanceOf(ExistingMemberException.class);
                }),
                DynamicTest.dynamicTest("존재하지 않는 아이디로 로그인하면 예외가 발생한다.", () -> {
                    assertThatThrownBy(() -> userService.login(32322L))
                            .isInstanceOf(NonExistingMemberException.class);
                }),
                DynamicTest.dynamicTest("회원 가입한 아이디로 로그인할 수 있다.", () -> {
                    SessionUser logined = userService.login(user.getSocialId());
                    Optional<User> foundUser = userRepository.findById(logined.getId());

                    assertAll(() -> assertTrue(foundUser.isPresent()),
                            () -> assertEquals(user.getName(), foundUser.get().getName()),
                            () -> assertEquals(user.getPhoneNumber(), foundUser.get().getPhoneNumber()),
                            () -> assertEquals(user.isAdminStatus(), foundUser.get().isAdminStatus()),
                            () -> assertEquals(user.getSocialId(), foundUser.get().getSocialId()),
                            () -> assertEquals(user.getAccountNumber(), foundUser.get().getAccountNumber()),
                            () -> assertEquals(user.getBank(), foundUser.get().getBank())
                    );
                })
        );
    }
}
