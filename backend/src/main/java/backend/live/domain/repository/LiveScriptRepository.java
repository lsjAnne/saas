package backend.live.domain.repository;

import backend.live.model.LiveScript;

import java.util.List;

public interface LiveScriptRepository {

    LiveScript save(LiveScript liveScript);

    List<LiveScript> findByLivePlanId(String livePlanId);

    void deleteAll();
}

