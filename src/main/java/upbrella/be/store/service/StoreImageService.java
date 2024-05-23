package upbrella.be.store.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import upbrella.be.store.dto.response.AllImageUrlResponse;
import upbrella.be.store.dto.response.SingleImageUrlResponse;
import upbrella.be.store.entity.StoreDetail;
import upbrella.be.store.entity.StoreImage;
import upbrella.be.store.exception.NonExistingStoreImageException;
import upbrella.be.store.repository.StoreImageRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StoreImageService {

    private final S3Client s3Client;
    private final StoreImageRepository storeImageRepository;
    private final StoreDetailService storeDetailService;

    @Value("${AWS_S3_BUCKET}")
    private String bucketName;

    public StoreImageService(S3Client s3Client, StoreImageRepository storeImageRepository, @Lazy StoreDetailService storeDetailService) {

        this.s3Client = s3Client;
        this.storeImageRepository = storeImageRepository;
        this.storeDetailService = storeDetailService;
    }

    @Transactional
    @CacheEvict(value = "stores", key = "'allStores'")
    public String uploadFile(MultipartFile file, long storeDetailId, String randomId) {

        String fileName = file.getOriginalFilename() + randomId;
        String contentType = file.getContentType();

        // Upload file
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("store-image/" + fileName)
                .acl("public-read")
                .contentDisposition("inline")
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            String url = "https://file.upbrella.co.kr/store-image/" + fileName;
            saveStoreImage(url, storeDetailId);
            return url;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload file", e);
        }
    }

    @Transactional
    @CacheEvict(value = "stores", key = "'allStores'")
    public void deleteFile(long imageId) {

        StoreImage storeImage = storeImageRepository.findById(imageId)
                .orElseThrow(() -> new NonExistingStoreImageException("[ERROR] 해당 이미지가 존재하지 않습니다."));
        storeImageRepository.deleteById(imageId);
        deleteFileInS3(storeImage.getImageUrl());
    }

    public String createThumbnail(List<SingleImageUrlResponse> imageUrls) {

        return imageUrls.stream()
                .findFirst()
                .map(SingleImageUrlResponse::getImageUrl)
                .orElse(null);
    }

    public String makeRandomId() {

        return UUID.randomUUID().toString().substring(0, 10);
    }

    @Transactional(readOnly = true)
    public AllImageUrlResponse findAllImages(long storeId) {

        StoreDetail storeDetail = storeDetailService.findByStoreMetaId(storeId);

        return AllImageUrlResponse.of(storeId, storeDetail.getStoreImages().stream()
                .map(SingleImageUrlResponse::createImageUrlResponse)
                .collect(Collectors.toList()));
    }

    private void deleteFileInS3(String imgUrl) {

        String key = parseKey(imgUrl);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private void saveStoreImage(String imageUrl, long storeId) {

        StoreDetail storeDetail = storeDetailService.findByStoreMetaId(storeId);
        storeImageRepository.save(StoreImage.createStoreImage(storeDetail, imageUrl));
    }


    private String parseKey(String url) {

        String[] splitUrl = url.split("/");

        return splitUrl[splitUrl.length - 2] + "/" + splitUrl[splitUrl.length - 1];
    }
}
