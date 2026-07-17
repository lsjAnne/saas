package backend.live.domain.repository;

import backend.live.model.LiveProductItem;

import java.util.List;
import java.util.Optional;

public interface LiveProductItemRepository {

    LiveProductItem save(LiveProductItem liveProductItem);

    List<LiveProductItem> findByLivePlanId(String livePlanId);

    Optional<LiveProductItem> findByLiveProductItemId(String liveProductItemId);

    void deleteByLiveProductItemId(String liveProductItemId);

    void deleteAll();
}

