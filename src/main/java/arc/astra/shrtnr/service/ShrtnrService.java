package arc.astra.shrtnr.service;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ShrtnrService {

    private final CacheService cacheService;

    public ShrtnrService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public String shortenURL(@NonNull String url){
        int counter = cacheService.incrementCounter();
        String code = Base62Encoder.encode(counter);
        cacheService.setUrl(code, url);
        return code;
    }

    public  String redirectURL(@NonNull String code){
       return cacheService.getURL(code);
    }
}


