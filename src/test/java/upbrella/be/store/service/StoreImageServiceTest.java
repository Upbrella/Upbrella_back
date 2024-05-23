package upbrella.be.store.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import upbrella.be.store.dto.response.SingleImageUrlResponse;
import upbrella.be.store.entity.StoreDetail;
import upbrella.be.store.entity.StoreImage;
import upbrella.be.store.exception.NonExistingStoreImageException;
import upbrella.be.store.repository.StoreImageRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Transactional
@ExtendWith(MockitoExtension.class)
class StoreImageServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private StoreDetailService storeDetailService;
    @InjectMocks
    private StoreImageService storeImageService;


    @Test
    @DisplayName("파일 업로드 테스트")
    void uploadFileTest() {
        // given
        long storeDetailId = 1L;
        MockMultipartFile file = new MockMultipartFile("image", "filename.jpg", "image/jpg", "some-image".getBytes());
        StoreDetail storeDetail = StoreDetail.builder()
                .build();
        String randomId = storeImageService.makeRandomId();
        String expectedUrl = "https://file.upbrella.co.kr/store-image/filename.jpg" + randomId;

        given(storeDetailService.findByStoreMetaId(storeDetailId)).willReturn(storeDetail);
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).willReturn(PutObjectResponse.builder().build());

        // when
        String result = storeImageService.uploadFile(file, storeDetailId, randomId);


        // then
        Assertions.assertThat(result).isEqualTo(expectedUrl);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(storeImageRepository, times(1)).save(any(StoreImage.class));
    }

    @Test
    @DisplayName("새로 사진을 등록할 때 기존에 있던 사진들을 삭제하는 테스트")
    void deleteFileTest() {
        // given
        long testId = 1L;
        String testUrl = "http://mybucket.s3.amazonaws.com/myimage.jpg";
        StoreImage testImage = StoreImage.builder().id(testId).imageUrl(testUrl).build();

        when(storeImageRepository.findById(testId)).thenReturn(Optional.of(testImage));

        // When
        storeImageService.deleteFile(testId);

        // Then
        assertAll(
                () -> assertEquals(testId, testImage.getId()),
                () -> assertEquals(testUrl, testImage.getImageUrl())
        );
    }

    @Test
    @DisplayName("사진이 존재하지 않는데 삭제를 시도하는 경우 예외를 발생시킨다.")
    void notExistImageDeleteTest() {
        // given
        long imageId = 1L;

        // when


        // then
        assertThatThrownBy(() -> storeImageService.deleteFile(imageId))
                .isInstanceOf(NonExistingStoreImageException.class)
                .hasMessageContaining("해당 이미지가 존재하지 않습니다.");
    }

    @Nested
    @DisplayName("사용자는 ")
    class ThumbNail{

        @Test
        @DisplayName("사용자는 썸네일을 생성할 수 있다.")
        void createThumbnailTest() {
            // given
            SingleImageUrlResponse fist = SingleImageUrlResponse.builder()
                    .id(1L)
                    .imageUrl("https://null.s3.ap-northeast-2.amazonaws.com/store-image/filename.jpg")
                    .build();

            SingleImageUrlResponse second = SingleImageUrlResponse.builder()
                    .id(2L)
                    .imageUrl("https://null.s3.ap-northeast-2.amazonaws.com/store-image/filename.jpg")
                    .build();

            List<SingleImageUrlResponse> imageUrls = List.of(fist, second);

            // when
            String thumbnail = storeImageService.createThumbnail(imageUrls);

            // then
            assertThat(thumbnail).isEqualTo(imageUrls.get(0).getImageUrl());
        }

        @Test
        @DisplayName("사진이 등록되지 않은 경우는 썸네일을 생성하지 않는다.")
        void emptyThumbnailTest() {
            // given
            List<SingleImageUrlResponse> emptyImageUrls = List.of();

            // when
            String thumbnail = storeImageService.createThumbnail(emptyImageUrls);

            // then
            assertThat(thumbnail).isNull();
        }
    }
}
