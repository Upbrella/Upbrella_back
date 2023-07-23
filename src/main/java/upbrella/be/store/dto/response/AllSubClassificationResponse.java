package upbrella.be.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AllSubClassificationResponse {

    List<SingleSubClassificationResponse> subClassifications;
}
