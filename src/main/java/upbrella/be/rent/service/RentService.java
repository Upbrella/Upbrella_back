package upbrella.be.rent.service;

import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upbrella.be.rent.dto.response.HistoryInfoDto;
import upbrella.be.rent.dto.request.HistoryFilterRequest;
import upbrella.be.rent.dto.request.RentUmbrellaByUserRequest;
import upbrella.be.rent.dto.request.ReturnUmbrellaByUserRequest;
import upbrella.be.rent.dto.response.RentFormResponse;
import upbrella.be.rent.dto.response.RentalHistoriesPageResponse;
import upbrella.be.rent.dto.response.RentalHistoryResponse;
import upbrella.be.rent.dto.response.ReturnFormResponse;
import upbrella.be.rent.entity.ConditionReport;
import upbrella.be.rent.entity.History;
import upbrella.be.rent.exception.*;
import upbrella.be.rent.repository.RentRepository;
import upbrella.be.store.entity.StoreMeta;
import upbrella.be.store.service.StoreMetaService;
import upbrella.be.umbrella.entity.Umbrella;
import upbrella.be.umbrella.exception.MissingUmbrellaException;
import upbrella.be.umbrella.exception.NonExistingBorrowedHistoryException;
import upbrella.be.umbrella.service.UmbrellaService;
import upbrella.be.user.dto.response.AllHistoryResponse;
import upbrella.be.user.dto.response.SessionUser;
import upbrella.be.user.dto.response.SingleHistoryResponse;
import upbrella.be.user.entity.User;
import upbrella.be.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentService {

    private final UmbrellaService umbrellaService;
    private final StoreMetaService storeMetaService;
    private final ImprovementReportService improvementReportService;
    private final UserService userService;
    private final RentRepository rentRepository;
    private final ConditionReportService conditionReportService;
    private final LockerService lockerService;

    public RentFormResponse findRentForm(long umbrellaId) {

        Umbrella umbrella = umbrellaService.findUmbrellaById(umbrellaId);

        if (umbrella.validateCannotBeRented()) {
            throw new CannotBeRentedException("[ERROR] 해당 우산은 대여 불가능한 우산입니다.");
        }

        return RentFormResponse.of(umbrella);
    }

    public ReturnFormResponse findReturnForm(long storeId, User userToReturn, String salt, String signature) {

        StoreMeta storeMeta = storeMetaService.findStoreMetaById(storeId);

        lockerService.validateLockerSignature(storeMeta.getId(), salt, signature);

        History history = rentRepository.findByUserIdAndReturnedAtIsNull(userToReturn.getId())
                .orElseThrow(() -> new NonExistingUmbrellaForRentException("[ERROR] 해당 유저가 대여 중인 우산이 없습니다."));

        return ReturnFormResponse.of(storeMeta, history);
    }

    @Transactional
    public void addRental(RentUmbrellaByUserRequest rentUmbrellaByUserRequest, User userToRent) {
        userService.checkBlackList(userToRent.getId());

        rentRepository.findByUserIdAndReturnedAtIsNull(userToRent.getId())
                .ifPresent(history -> {
                    throw new ExistingUmbrellaForRentException("[ERROR] 해당 유저가 대여 중인 우산이 있습니다.");
                });

        Umbrella willRentUmbrella = umbrellaService.findUmbrellaById(rentUmbrellaByUserRequest.getUmbrellaId());
        if(willRentUmbrella.getStoreMeta().getId() != rentUmbrellaByUserRequest.getStoreId()){
            throw new UmbrellaStoreMissMatchException("[ERROR] 해당 우산은 해당 매장에 존재하지 않습니다.");
        }
        if (willRentUmbrella.isMissed()) {
            throw new MissingUmbrellaException("[ERROR] 해당 우산은 분실되었습니다.");
        }
        if (!willRentUmbrella.isRentable()){
            throw new NotAvailableUmbrellaException("[ERROR] 해당 우산은 대여중입니다.");
        }

        willRentUmbrella.rentUmbrella();
        StoreMeta rentalStore = storeMetaService.findStoreMetaById(rentUmbrellaByUserRequest.getStoreId());

        String conditionReport = rentUmbrellaByUserRequest.getConditionReport();

        History history = rentRepository.save(History.ofCreatedByNewRent(willRentUmbrella, userToRent, rentalStore));

        ConditionReport conditionReportToSave = ConditionReport.builder()
                .content(conditionReport)
                .history(history)
                .build();

        conditionReportService.saveConditionReport(conditionReportToSave);
    }

    @Transactional
    public void returnUmbrellaByUser(User userToReturn, ReturnUmbrellaByUserRequest request) {

        // 반납일 때 secretKey.salt 대문자 후 SHA256 해싱 -> signature와 검증 후, 검증 실패 시 예외 발생
        // 보관함이 없는 store일때 salt와 signature 입력 시 예외발생

        userService.checkBlackList(userToReturn.getId());
        History history = rentRepository.findByUserIdAndReturnedAtIsNull(userToReturn.getId())
                .orElseThrow(() -> new NonExistingUmbrellaForRentException("[ERROR] 해당 유저가 대여 중인 우산이 없습니다."));

        StoreMeta returnStore = storeMetaService.findStoreMetaById(request.getReturnStoreId());

        History updatedHistory = History.updateHistoryForReturn(history, returnStore, request);
        Umbrella returnedUmbrella = history.getUmbrella();
        returnedUmbrella.returnUmbrella(returnStore);

        rentRepository.save(updatedHistory);
        addImprovementReportFromReturnByUser(updatedHistory, request);
    }

    @Transactional
    public RentalHistoriesPageResponse findAllHistories(HistoryFilterRequest filter, Pageable pageable) {

        long countOfAllHistories = rentRepository.countAll(filter, pageable);
        long countOfAllPages = countOfAllHistories / pageable.getPageSize();

        return RentalHistoriesPageResponse.of(findAllRentalHistory(filter, pageable), countOfAllHistories, countOfAllPages);
    }

    public AllHistoryResponse findAllHistoriesByUser(long userId) {

        return AllHistoryResponse.of(findAllByUserId(userId));
    }

    private void addImprovementReportFromReturnByUser(History history, ReturnUmbrellaByUserRequest request) {

        if (request.getImprovementReportContent() == null) {
            return;
        }

        improvementReportService.addImprovementReportFromReturn(history, request.getImprovementReportContent());
    }

    private List<SingleHistoryResponse> findAllByUserId(long userId) {

        return findAllByUser(userId)
                .stream()
                .map(this::toSingleHistoryResponse)
                .collect(Collectors.toList());
    }

    private List<History> findAllByUser(long userId) {

        return rentRepository.findAllByUserId(userId);
    }

    private List<RentalHistoryResponse> findAllRentalHistory(HistoryFilterRequest filter, Pageable pageable) {

        return findHistoryInfos(filter, pageable).stream()
                .map(this::toRentalHistoryResponse)
                .collect(Collectors.toList());
    }

    private List<History> findAll(HistoryFilterRequest filter, Pageable pageable) {

        return rentRepository.findAll(filter, pageable);
    }

    private SingleHistoryResponse toSingleHistoryResponse(History history) {

        boolean isReturned = true;
        boolean isRefunded = false;
        LocalDateTime returnAt = history.getReturnedAt();

        if (returnAt == null) {
            isReturned = false;
            returnAt = history.getRentedAt().plusDays(14);
        }

        if (history.getRefundedAt() != null) {
            isRefunded = true;
        }

        return SingleHistoryResponse.ofUserHistory(history, returnAt, isReturned, isRefunded);
    }

    private RentalHistoryResponse toRentalHistoryResponse(HistoryInfoDto history) {

        int elapsedDay = (int) ChronoUnit.DAYS.between(history.getRentAt(), LocalDateTime.now());
        int totalRentalDay;

        if (history.getReturnAt() != null) {

            elapsedDay = history.getReturnAt().getDayOfYear() - history.getRentAt().getDayOfYear();
            totalRentalDay = history.getReturnAt().getDayOfYear() - history.getRentAt().getDayOfYear();

            return RentalHistoryResponse.createReturnedHistory(history, elapsedDay, totalRentalDay);
        }

        return RentalHistoryResponse.createNonReturnedHistory(history, elapsedDay);
    }

    public long countTotalRent() {

        return rentRepository.count();
    }

    public long countTotalRentByStoreId(long storeId) {

        return rentRepository.countByRentStoreMetaId(storeId);
    }

    public long countUnrefundedRent() {

        return rentRepository.countAllByReturnedAtIsNotNullAndPaidAtIsNotNullAndRefundedAtIsNull();
    }

    @Transactional
    public void checkRefund(long historyId, long userId) {

        User loginedUser = userService.findUserById(userId);

        History history = findHistoryById(historyId);

        history.refund(loginedUser, LocalDateTime.now());
        rentRepository.save(history);
    }

    @Transactional
    public void checkPayment(long historyId, long userId) {

        User loginedUser = userService.findUserById(userId);

        History history = findHistoryById(historyId);
        history.paid(loginedUser, LocalDateTime.now());
        rentRepository.save(history);
    }

    private History findHistoryById(long historyId) {

        return rentRepository.findById(historyId)
                .orElseThrow(() -> new NonExistingHistoryException("[ERROR] 해당 대여 기록이 없습니다."));
    }

    public History findRentalHistoryByUser(SessionUser sessionUser) {

        return rentRepository.findByUserIdAndReturnedAtIsNull(sessionUser.getId())
                .orElseThrow(() -> new NonExistingBorrowedHistoryException("[ERROR] 사용자가 빌린 우산이 없습니다."));
    }

    @Transactional
    public void deleteBankAccount(long historyId) {

        History history = findHistoryById(historyId);

        history.deleteBankAccount();
    }

    private List<HistoryInfoDto> findHistoryInfos(HistoryFilterRequest filter, Pageable pageable) {

        return rentRepository.findHistoryInfos(filter, pageable);
    }
}
