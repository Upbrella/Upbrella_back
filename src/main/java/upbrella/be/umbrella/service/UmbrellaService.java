package upbrella.be.umbrella.service;

import org.springframework.stereotype.Service;
import upbrella.be.umbrella.dto.request.UmbrellaRequest;
import upbrella.be.umbrella.dto.response.UmbrellaResponse;

import java.util.List;

@Service
public class UmbrellaService {
    public List<UmbrellaResponse> findAllUmbrellas() {
        return null;
    }
    public List<UmbrellaResponse> findUmbrellasByStoreId(long storeId) {
        return null;
    }

    public void addUmbrella(UmbrellaRequest umbrellaRequest) {}
    public void modifyUmbrella(long id, UmbrellaRequest umbrellaRequest) {}
    public void deleteUmbrella(long id) {}

}
