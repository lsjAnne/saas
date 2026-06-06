package com.dianshang.platform.live.domain.repository;

import com.dianshang.platform.live.model.LiveScript;

import java.util.List;

public interface LiveScriptRepository {

    LiveScript save(LiveScript liveScript);

    List<LiveScript> findByLivePlanId(String livePlanId);

    void deleteAll();
}
