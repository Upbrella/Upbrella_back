package upbrella.be.umbrella.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import upbrella.be.config.FixtureBuilderFactory;
import upbrella.be.config.FixtureFactory;
import upbrella.be.store.entity.StoreMeta;
import upbrella.be.store.exception.NonExistingStoreMetaException;
import upbrella.be.store.service.StoreMetaService;
import upbrella.be.umbrella.dto.request.UmbrellaCreateRequest;
import upbrella.be.umbrella.dto.request.UmbrellaModifyRequest;
import upbrella.be.umbrella.dto.response.UmbrellaResponse;
import upbrella.be.umbrella.dto.response.UmbrellaStatisticsResponse;
import upbrella.be.umbrella.entity.Umbrella;
import upbrella.be.umbrella.exception.ExistingUmbrellaUuidException;
import upbrella.be.umbrella.exception.NonExistingUmbrellaException;
import upbrella.be.umbrella.repository.UmbrellaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UmbrellaServiceTest {

    @Mock
    private UmbrellaRepository umbrellaRepository;
    @Mock
    private StoreMetaService storeMetaService;
    @InjectMocks
    private UmbrellaService umbrellaService;

    @Nested
    @DisplayName("페이지 번호와 페이지 크기를 입력받아")
    class findAllUmbrellasTest {
        private StoreMeta storeMeta;
        private List<UmbrellaResponse> expectedUmbrellaResponses;
        private List<Umbrella> generatedUmbrellas = new ArrayList<>();

        @BeforeEach
        void setUp() {

            storeMeta = FixtureBuilderFactory.builderStoreMeta().sample();

            for (int i = 0; i < 5; i++) {
                generatedUmbrellas.add(FixtureBuilderFactory.builderUmbrella()
                        .set("storeMeta", storeMeta)
                        .sample());
            }

            expectedUmbrellaResponses = generatedUmbrellas.stream()
                    .map(umbrella -> FixtureFactory.buildUmbrellaResponseWithUmbrellaAndStoreMeta(umbrella, storeMeta))
                    .collect(Collectors.toList());
        }

        @DisplayName("해당하는 페이지의 우산 고유번호, 우산 관리번호, 협력 지점 고유번호, 대여 가능 여부를 포함하는 우산 목록 정보를 반환한다.")
        @Test
        void success() {

            //given
            Pageable pageable = PageRequest.of(0, 5);
            given(umbrellaRepository.findByDeletedIsFalseOrderById(pageable))
                    .willReturn(generatedUmbrellas);

            //when
            List<UmbrellaResponse> umbrellaResponseList = umbrellaService.findAllUmbrellas(pageable);

            //then
            assertAll(
                    () -> assertThat(umbrellaResponseList.size())
                            .isEqualTo(expectedUmbrellaResponses.size()),
                    () -> assertThat(umbrellaResponseList)
                            .usingRecursiveComparison()
                            .isEqualTo(expectedUmbrellaResponses)
            );
        }

        @DisplayName("해당하는 우산이 없으면 빈 객체를 반환한다.")
        @Test
        void empty() {

            //given
            Pageable pageable = PageRequest.of(0, 5);
            given(umbrellaRepository.findByDeletedIsFalseOrderById(pageable))
                    .willReturn(List.of());

            //when
            List<UmbrellaResponse> umbrellaResponseList = umbrellaService.findAllUmbrellas(pageable);

            //then
            assertThat(umbrellaResponseList).isEmpty();
        }
    }

    @Nested
    @DisplayName("협력 지점의 고유번호와 페이지 번호, 페이지 크기를 입력받아")
    class findUmbrellasByStoreIdTest {
        private StoreMeta storeMeta;
        private List<UmbrellaResponse> expectedUmbrellaResponses;
        private List<Umbrella> generatedUmbrellas = new ArrayList<>();

        @BeforeEach
        void setUp() {

            storeMeta = FixtureBuilderFactory.builderStoreMeta().sample();

            for (int i = 0; i < 5; i++) {
                generatedUmbrellas.add(FixtureBuilderFactory.builderUmbrella()
                        .set("storeMeta", storeMeta)
                        .sample());
            }

            expectedUmbrellaResponses = generatedUmbrellas.stream()
                    .map(umbrella -> FixtureFactory.buildUmbrellaResponseWithUmbrellaAndStoreMeta(umbrella, storeMeta))
                    .collect(Collectors.toList());
        }

        @DisplayName("해당하는 페이지의 우산 고유번호, 우산 관리번호, 협력 지점 고유번호, 대여 가능 여부를 포함하는 우산 목록 정보를 반환한다.")
        @Test
        void success() {
            //given
            Pageable pageable = PageRequest.of(0, 5);
            given(umbrellaRepository.findByStoreMetaIdAndDeletedIsFalseOrderById(2L, pageable))
                    .willReturn(generatedUmbrellas);

            //when
            List<UmbrellaResponse> umbrellaResponseList = umbrellaService.findUmbrellasByStoreId(2L, pageable);

            //then
            assertAll(
                    () -> assertThat(umbrellaResponseList.size())
                            .isEqualTo(expectedUmbrellaResponses.size()),
                    () -> assertThat(umbrellaResponseList)
                            .usingRecursiveComparison()
                            .isEqualTo(expectedUmbrellaResponses)
            );
        }

        @DisplayName("해당하는 우산이 없으면 빈 객체를 반환한다.")
        @Test
        void empty() {

            //given
            Pageable pageable = PageRequest.of(0, 5);
            given(umbrellaRepository.findByStoreMetaIdAndDeletedIsFalseOrderById(2L, pageable))
                    .willReturn(List.of());

            //when
            List<UmbrellaResponse> umbrellaResponseList = umbrellaService.findUmbrellasByStoreId(2L, pageable);

            //then
            assertThat(umbrellaResponseList).isEmpty();
        }
    }

    @Nested
    @DisplayName("우산의 고유번호, 협력 지점 고유번호, 대여 여부를 입력받아")
    class addUmbrellaTest {
        private UmbrellaCreateRequest umbrellaCreateRequest;
        private StoreMeta foundStoreMeta;
        private Umbrella umbrella;

        @BeforeEach
        void setUp() {
            umbrellaCreateRequest = FixtureBuilderFactory.builderUmbrellaCreateRequest().sample();

            foundStoreMeta = FixtureFactory.buildStoreMetaWithId(umbrellaCreateRequest.getStoreMetaId());

            umbrella = FixtureFactory.buildUmbrellaWithUmbrellaRequestAndStoreMeta(umbrellaCreateRequest, foundStoreMeta);
        }

        @Test
        @DisplayName("우산을 정상적으로 추가할 수 있다.")
        void success() {

            // given
            given(storeMetaService.findStoreMetaById(foundStoreMeta.getId()))
                    .willReturn(foundStoreMeta);
            given(umbrellaRepository.existsByUuidAndDeletedIsFalse(umbrellaCreateRequest.getUuid()))
                    .willReturn(false);
            given(umbrellaRepository.save(any(Umbrella.class)))
                    .willReturn(umbrella);

            // when
            Umbrella addedUmbrella = umbrellaService.addUmbrella(umbrellaCreateRequest);

            // then
            assertAll(
                    () -> assertThat(addedUmbrella)
                            .usingRecursiveComparison()
                            .isEqualTo(umbrella),
                    () -> then(umbrellaRepository).should(times(1))
                            .existsByUuidAndDeletedIsFalse(umbrellaCreateRequest.getUuid()),
                    () -> then(storeMetaService).should(times(1))
                            .findStoreMetaById(foundStoreMeta.getId()),
                    () -> then(umbrellaRepository).should(times(1))
                            .save(any(Umbrella.class))
            );
        }

        @Test
        @DisplayName("우산 고유번호가 이미 존재하는 경우 예외를 발생시킨다.")
        void withSameId() {

            // given
            given(storeMetaService.findStoreMetaById(foundStoreMeta.getId()))
                    .willReturn(foundStoreMeta);
            given(umbrellaRepository.existsByUuidAndDeletedIsFalse(umbrella.getUuid()))
                    .willReturn(true);

            // when
            assertThatThrownBy(() -> umbrellaService.addUmbrella(umbrellaCreateRequest))
                    .isInstanceOf(ExistingUmbrellaUuidException.class);

            // then
            assertAll(
                    () -> then(storeMetaService).should(times(1))
                            .findStoreMetaById(foundStoreMeta.getId()),
                    () -> then(umbrellaRepository).should(times(1))
                            .existsByUuidAndDeletedIsFalse(umbrellaCreateRequest.getUuid()),
                    () -> then(umbrellaRepository).should(never())
                            .save(any(Umbrella.class))
            );
        }

        @Test
        @DisplayName("추가하려고 하는 가게 고유번호가 존재하지 않는 경우 예외를 발생시킨다.")
        void atNonExistingStore() {

            // given
            given(storeMetaService.findStoreMetaById(foundStoreMeta.getId()))
                    .willThrow(new NonExistingStoreMetaException("[ERROR] 존재하지 않는 협업지점 ID입니다."));

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() -> umbrellaService.addUmbrella(umbrellaCreateRequest))
                            .isInstanceOf(NonExistingStoreMetaException.class),
                    () -> then(storeMetaService).should(times(1))
                            .findStoreMetaById(foundStoreMeta.getId()),
                    () -> then(umbrellaRepository).shouldHaveNoInteractions()
            );
        }
    }

    @Nested
    @DisplayName("우산의 고유번호, 협력 지점 고유번호, 대여 여부를 입력받아")
    class modifyUmbrellaTest {
        private UmbrellaModifyRequest umbrellaModifyRequest;
        private StoreMeta foundStoreMeta;
        private Umbrella umbrella;
        private long id;

        @BeforeEach
        void setUp() {

            id = FixtureBuilderFactory.buildLong(1000);

            umbrellaModifyRequest = FixtureBuilderFactory.builderUmbrellaModifyRequest().sample();

            foundStoreMeta = FixtureFactory.buildStoreMetaWithId(umbrellaModifyRequest.getStoreMetaId());

            umbrella = FixtureFactory.buildUmbrellaWithIdAndUmbrellaRequestAndStoreMeta(id, umbrellaModifyRequest, foundStoreMeta);
        }

        @Test
        @DisplayName("우산을 정상적으로 수정한다.")
        void success() {

            // given
            given(storeMetaService.findStoreMetaById(foundStoreMeta.getId()))
                    .willReturn(foundStoreMeta);
            given(umbrellaRepository.existsByIdAndDeletedIsFalse(id))
                    .willReturn(true);
            given(umbrellaRepository.existsByUuidAndDeletedIsFalse(umbrellaModifyRequest.getUuid()))
                    .willReturn(false);
            given(umbrellaRepository.save(any(Umbrella.class)))
                    .willReturn(umbrella);

            // when
            Umbrella modifiedUmbrella = umbrellaService.modifyUmbrella(umbrella.getId(), umbrellaModifyRequest);

            // then
            assertAll(
                    () -> assertThat(modifiedUmbrella)
                            .usingRecursiveComparison()
                            .isEqualTo(umbrella),
                    () -> then(umbrellaRepository).should(times(1))
                            .existsByUuidAndDeletedIsFalse(umbrellaModifyRequest.getUuid()),
                    () -> then(umbrellaRepository).should(times(1))
                            .existsByIdAndDeletedIsFalse(id),
                    () -> then(storeMetaService).should(times(1))
                            .findStoreMetaById(foundStoreMeta.getId()),
                    () -> then(umbrellaRepository).should(times(1))
                            .save(any(Umbrella.class))
            );
        }

        @Test
        @DisplayName("수정하려는 우산 고유번호가 존재하지 않는 경우 예외를 발생시킨다.")
        void withNonExistingId() {

            // given
            given(storeMetaService.findStoreMetaById(foundStoreMeta.getId()))
                    .willReturn(foundStoreMeta);
            given(umbrellaRepository.existsByIdAndDeletedIsFalse(id))
                    .willReturn(false);

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() ->
                            umbrellaService.modifyUmbrella(id, umbrellaModifyRequest))
                            .isInstanceOf(NonExistingUmbrellaException.class),
                    () -> then(umbrellaRepository).should(never())
                            .existsByUuidAndDeletedIsFalse(umbrellaModifyRequest.getUuid()),
                    () -> then(umbrellaRepository).should(times(1))
                            .existsByIdAndDeletedIsFalse(id),
                    () -> then(storeMetaService).should(times(1))
                            .findStoreMetaById(foundStoreMeta.getId()),
                    () -> then(umbrellaRepository).should(never())
                            .save(any(Umbrella.class))
            );
        }

        @Test
        @DisplayName("수정하려는 우산 관리번호가 이미 존재하는 경우 예외를 발생시킨다.")
        void withAlreadyExistingUuid() {

            // given
            given(storeMetaService.findStoreMetaById(foundStoreMeta.getId()))
                    .willReturn(foundStoreMeta);
            given(umbrellaRepository.existsByIdAndDeletedIsFalse(id))
                    .willReturn(true);
            given(umbrellaRepository.existsByUuidAndDeletedIsFalse(umbrellaModifyRequest.getUuid()))
                    .willReturn(true);

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() ->
                            umbrellaService.modifyUmbrella(id, umbrellaModifyRequest))
                            .isInstanceOf(ExistingUmbrellaUuidException.class),
                    () -> then(umbrellaRepository).should(times(1))
                            .existsByUuidAndDeletedIsFalse(umbrellaModifyRequest.getUuid()),
                    () -> then(umbrellaRepository).should(times(1))
                            .existsByIdAndDeletedIsFalse(id),
                    () -> then(storeMetaService).should(times(1))
                            .findStoreMetaById(foundStoreMeta.getId()),
                    () -> then(umbrellaRepository).should(never())
                            .save(any(Umbrella.class))
            );
        }

        @Test
        @DisplayName("추가하려고 하는 가게 고유번호가 존재하지 않는 경우 예외를 발생시킨다.")
        void atNonExistingStore() {

            // given
            given(storeMetaService.findStoreMetaById(foundStoreMeta.getId()))
                    .willThrow(new NonExistingStoreMetaException("[ERROR] 존재하지 않는 협업 지점 ID입니다."));

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() ->
                            umbrellaService.modifyUmbrella(id, umbrellaModifyRequest))
                            .isInstanceOf(NonExistingStoreMetaException.class),
                    () -> then(storeMetaService).should(times(1))
                            .findStoreMetaById(foundStoreMeta.getId()),
                    () -> then(umbrellaRepository).shouldHaveNoInteractions()
            );
        }
    }

    @Nested
    @DisplayName("우산의 고유번호를 입력받아")
    class deleteUmbrellaTest {

        private long id;
        private Umbrella umbrella;

        @BeforeEach
        void setUp() {

            id = FixtureBuilderFactory.buildLong(1000);
            umbrella = FixtureBuilderFactory.builderUmbrella().sample();
        }

        @Test
        @DisplayName("우산을 정상적으로 삭제한다.")
        void success() {

            // given
            given(umbrellaRepository.findByIdAndDeletedIsFalse(id))
                    .willReturn(Optional.of(umbrella));

            // when
            umbrellaService.deleteUmbrella(id);

            // then
            assertAll(
                    () -> assertThat(umbrella.isDeleted())
                            .isEqualTo(true),
                    () -> then(umbrellaRepository).should(times(1))
                            .findByIdAndDeletedIsFalse(id)
            );
        }

        @Test
        @DisplayName("우산이 이미 삭제되었거나 고유번호가 존재하지 않는 경우 예외를 발생시킨다.")
        void alreadyDeletedOrNonExistingId() {

            // given
            given(umbrellaRepository.findByIdAndDeletedIsFalse(id))
                    .willReturn(Optional.ofNullable(null));

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() -> umbrellaService.deleteUmbrella(id))
                            .isInstanceOf(NonExistingUmbrellaException.class),
                    () -> then(umbrellaRepository).should(times(1))
                            .findByIdAndDeletedIsFalse(id)
            );
        }
    }

    @Nested
    @DisplayName("협업 지점의 고유 번호를 입력받아")
    class countAvailableUmbrellaAtStoreTest {

        @Test
        @DisplayName("해당 협업 지점에서 현재 이용 가능한 우산의 개수를 반환한다.")
        void success() {

            // given
            long id = FixtureBuilderFactory.buildLong(1000);
            int availableCount = FixtureBuilderFactory.buildInteger(100);
            given(umbrellaRepository.countUmbrellasByStoreMetaIdAndRentableIsTrueAndMissedIsFalseAndDeletedIsFalse(id))
                    .willReturn(availableCount);

            // when
            int count = umbrellaService.countAvailableUmbrellaAtStore(id);

            // then
            assertAll(
                    () -> assertThat(count)
                            .isEqualTo(availableCount),
                    () -> then(umbrellaRepository).should(times(1))
                            .countUmbrellasByStoreMetaIdAndRentableIsTrueAndMissedIsFalseAndDeletedIsFalse(id)
            );
        }
    }

    @Test
    @DisplayName("전체 우산의 통계를 조회할 수 있다.")
    void getUmbrellaAllStatisticsTest() {

        // given
        UmbrellaStatisticsResponse expected = FixtureBuilderFactory.builderUmbrellaStatisticsResponse()
                .sample();

        int totalCount = expected.getTotalUmbrellaCount();
        int rentableCount = expected.getRentableUmbrellaCount();
        int rentedCount = expected.getRentedUmbrellaCount();
        int missingCount = expected.getMissingUmbrellaCount();

        given(umbrellaRepository.countUmbrellaByDeletedIsFalse())
                .willReturn(totalCount);
        given(umbrellaRepository.countUmbrellasByRentableIsTrueAndMissedIsFalseAndDeletedIsFalse())
                .willReturn(rentableCount);
        given(umbrellaRepository.countUmbrellasByRentableIsFalseAndMissedIsFalseAndDeletedIsFalse())
                .willReturn(rentedCount);
        given(umbrellaRepository.countUmbrellasByMissedIsTrueAndDeletedIsFalse())
                .willReturn(missingCount);

        // when
        UmbrellaStatisticsResponse umbrellaAllStatistics = umbrellaService.getUmbrellaAllStatistics();

        // then
        assertAll(
                () -> assertThat(umbrellaAllStatistics)
                        .usingRecursiveComparison()
                        .isEqualTo(expected),
                () -> then(umbrellaRepository).should(times(1))
                        .countUmbrellaByDeletedIsFalse(),
                () -> then(umbrellaRepository).should(times(1))
                        .countUmbrellasByRentableIsTrueAndMissedIsFalseAndDeletedIsFalse(),
                () -> then(umbrellaRepository).should(times(1))
                        .countUmbrellasByRentableIsFalseAndMissedIsFalseAndDeletedIsFalse(),
                () -> then(umbrellaRepository).should(times(1))
                        .countUmbrellasByMissedIsTrueAndDeletedIsFalse());
    }

    @Nested
    @DisplayName("협업 지점의 고유 번호를 입력받아")
    class getUmbrellaStatisticsByStoreTest {

        @Test
        @DisplayName("지점 우산의 통계를 조회할 수 있다.")
        void success() {

            // given
            UmbrellaStatisticsResponse expected = FixtureBuilderFactory.builderUmbrellaStatisticsResponse()
                    .sample();

            int totalCount = expected.getTotalUmbrellaCount();
            int rentableCount = expected.getRentableUmbrellaCount();
            int rentedCount = expected.getRentedUmbrellaCount();
            int missingCount = expected.getMissingUmbrellaCount();
            long storeId = FixtureBuilderFactory.buildLong(1000);

            given(storeMetaService.existByStoreId(storeId))
                    .willReturn(true);
            given(umbrellaRepository.countUmbrellaByStoreMetaIdAndDeletedIsFalse(storeId))
                    .willReturn(totalCount);
            given(umbrellaRepository.countUmbrellasByStoreMetaIdAndRentableIsTrueAndMissedIsFalseAndDeletedIsFalse(storeId))
                    .willReturn(rentableCount);
            given(umbrellaRepository.countUmbrellasByStoreMetaIdAndRentableIsFalseAndMissedIsFalseAndDeletedIsFalse(storeId))
                    .willReturn(rentedCount);
            given(umbrellaRepository.countUmbrellasByStoreMetaIdAndMissedIsTrueAndDeletedIsFalse(storeId))
                    .willReturn(missingCount);

            // when
            UmbrellaStatisticsResponse umbrellaStatistics = umbrellaService.getUmbrellaStatisticsByStoreId(storeId);

            // then
            assertAll(
                    () -> assertThat(umbrellaStatistics)
                            .usingRecursiveComparison()
                            .isEqualTo(expected),
                    () -> then(umbrellaRepository).should(times(1))
                            .countUmbrellaByStoreMetaIdAndDeletedIsFalse(storeId),
                    () -> then(umbrellaRepository).should(times(1))
                            .countUmbrellasByStoreMetaIdAndRentableIsTrueAndMissedIsFalseAndDeletedIsFalse(storeId),
                    () -> then(umbrellaRepository).should(times(1))
                            .countUmbrellasByStoreMetaIdAndRentableIsFalseAndMissedIsFalseAndDeletedIsFalse(storeId),
                    () -> then(umbrellaRepository).should(times(1))
                            .countUmbrellasByStoreMetaIdAndMissedIsTrueAndDeletedIsFalse(storeId),
                    () -> then(storeMetaService).should(times(1))
                            .existByStoreId(storeId));
        }

        @Test
        @DisplayName("협업 지점이 존재하지 않는 경우 예외를 발생시킨다.")
        void nonExistingStore() {

            // given
            long storeId = FixtureBuilderFactory.buildLong(1000);
            given(storeMetaService.existByStoreId(storeId))
                    .willReturn(false);

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() -> umbrellaService.getUmbrellaStatisticsByStoreId(storeId))
                            .isInstanceOf(NonExistingStoreMetaException.class),
                    () -> then(storeMetaService).should(times(1))
                            .existByStoreId(storeId)
            );
        }
    }


}
