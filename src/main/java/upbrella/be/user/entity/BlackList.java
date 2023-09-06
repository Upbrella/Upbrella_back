package upbrella.be.user.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BlackList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long socialId;
    private LocalDateTime blockedAt;

    public static BlackList createNewBlackList(Long socialId) {

        return BlackList.builder()
                .socialId(socialId.hashCode())
                .blockedAt(LocalDateTime.now())
                .build();
    }
}
