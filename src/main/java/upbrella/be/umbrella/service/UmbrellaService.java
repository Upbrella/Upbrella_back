package upbrella.be.umbrella.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import upbrella.be.store.entity.StoreMeta;
import upbrella.be.store.exception.NonExistingStoreMetaException;
import upbrella.be.store.service.StoreMetaService;
import upbrella.be.umbrella.dto.request.UmbrellaCreateRequest;
import upbrella.be.umbrella.dto.request.UmbrellaModifyRequest;
import upbrella.be.umbrella.dto.response.UmbrellaStatisticsResponse;
import upbrella.be.umbrella.dto.response.UmbrellaResponse;
import upbrella.be.umbrella.entity.Umbrella;
import upbrella.be.umbrella.exception.ExistingUmbrellaUuidException;
import upbrella.be.umbrella.exception.NonExistingUmbrellaException;
import upbrella.be.umbrella.repository.UmbrellaRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UmbrellaService {
    private final UmbrellaRepository umbrellaRepository;
    private final StoreMetaService storeMetaService;

    public List<UmbrellaResponse> findAllUmbrellas(Pageable pageable) {

        return umbrellaRepository.findByDeletedIsFalseOrderById(pageable)
                .stream().map(UmbrellaResponse::fromUmbrella)
                .collect(Collectors.toList());
    }

    public List<UmbrellaResponse> findUmbrellasByStoreId(long storeId, Pageable pageable) {

        return umbrellaRepository.findByStoreMetaIdAndDeletedIsFalseOrderById(storeId, pageable)
                .stream().map(UmbrellaResponse::fromUmbrella)
                .collect(Collectors.toList());
    }

    @Transactional
    public Umbrella addUmbrella(UmbrellaCreateRequest umbrellaCreateRequest) {

        StoreMeta storeMeta = storeMetaService.findStoreMetaById(umbrellaCreateRequest.getStoreMetaId());

        if (umbrellaRepository.existsByUuidAndDeletedIsFalse(umbrellaCreateRequest.getUuid())) {
            throw new ExistingUmbrellaUuidException("[ERROR] 이미 존재하는 우산 관리 번호입니다.");
        }

        return umbrellaRepository.save(
                Umbrella.ofCreated(umbrellaCreateRequest, storeMeta)
        );
    }

    @Transactional
    public Umbrella modifyUmbrella(long id, UmbrellaModifyRequest umbrellaModifyRequest) {

        StoreMeta storeMeta = storeMetaService.findStoreMetaById(umbrellaModifyRequest.getStoreMetaId());

        if (!umbrellaRepository.existsByIdAndDeletedIsFalse(id)) {
            throw new NonExistingUmbrellaException("[ERROR] 존재하지 않는 우산 고유번호입니다.");
        }
        if (umbrellaRepository.existsByUuidAndDeletedIsFalse(umbrellaModifyRequest.getUuid())) {
            throw new ExistingUmbrellaUuidException("[ERROR] 이미 존재하는 우산 관리 번호입니다.");
        }

        return umbrellaRepository.save(
                Umbrella.ofUpdated(id, umbrellaModifyRequest, storeMeta)
        );
    }

    @Transactional
    public void deleteUmbrella(long id) {

        Umbrella foundUmbrella = findUmbrellaById(id);
        foundUmbrella.delete();
    }

    public Umbrella findUmbrellaById(long id) {

        return umbrellaRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new NonExistingUmbrellaException("[ERROR] 존재하지 않는 우산 고유번호입니다."));
    }

    public int countAvailableUmbrellaAtStore(long storeMetaId) {

        return umbrellaRepository.countUmbrellasByStoreMetaIdAndRentableIsTrueAndMissedIsFalseAndDeletedIsFalse(storeMetaId);
    }

    public UmbrellaStatisticsResponse getUmbrellaAllStatistics() {

        int totalUmbrella = countUmbrella();
        int availableUmbrella = countAvailableUmbrella();
        int rentedUmbrella = countRentedUmbrella();
        int missingUmbrella = countMissingUmberlla();

        return UmbrellaStatisticsResponse.fromCounts(totalUmbrella, availableUmbrella, rentedUmbrella, missingUmbrella);
    }

    public UmbrellaStatisticsResponse getUmbrellaStatisticsByStoreId(long storeId) {

        if (!storeMetaService.existByStoreId(storeId)) {
            throw new NonExistingStoreMetaException("[ERROR] 존재하지 않는 매장 고유번호입니다.");
        }

        int totalUmbrellaByStoreId = countUmbrellaByStoreId(storeId);
        int availableUmbrellaByStoreId = countAvailableUmbrellaByStoreId(storeId);
        int rentedUmbrellaByStoreId = countRentedUmbrellaByStoreId(storeId);
        int missingUmbrellaByStoreId = countMissingUmberllaByStoreId(storeId);

        return UmbrellaStatisticsResponse.fromCounts(totalUmbrellaByStoreId, availableUmbrellaByStoreId,
                rentedUmbrellaByStoreId, missingUmbrellaByStoreId);
    }

    private int countAvailableUmbrella() {

        return umbrellaRepository.countUmbrellasByRentableIsTrueAndMissedIsFalseAndDeletedIsFalse();
    }

    private int countRentedUmbrella() {

        return umbrellaRepository.countUmbrellasByRentableIsFalseAndMissedIsFalseAndDeletedIsFalse();
    }

    private int countUmbrella() {

        return umbrellaRepository.countUmbrellaByDeletedIsFalse();
    }

    private int countMissingUmberlla() {

        return umbrellaRepository.countUmbrellasByMissedIsTrueAndDeletedIsFalse();
    }

    private int countAvailableUmbrellaByStoreId(long storeId) {

        return umbrellaRepository.countUmbrellasByStoreMetaIdAndRentableIsTrueAndMissedIsFalseAndDeletedIsFalse(storeId);
    }

    private int countRentedUmbrellaByStoreId(long storeId) {

        return umbrellaRepository.countUmbrellasByStoreMetaIdAndRentableIsFalseAndMissedIsFalseAndDeletedIsFalse(storeId);
    }

    private int countUmbrellaByStoreId(long storeId) {

        return umbrellaRepository.countUmbrellaByStoreMetaIdAndDeletedIsFalse(storeId);
    }

    private int countMissingUmberllaByStoreId(long storeId) {

        return umbrellaRepository.countUmbrellasByStoreMetaIdAndMissedIsTrueAndDeletedIsFalse(storeId);
    }
}
