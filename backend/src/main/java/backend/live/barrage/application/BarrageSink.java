package backend.live.barrage.application;

import backend.live.barrage.model.LiveBarrageMessage;

/**
 * 弹幕接收回调：连接器把拉取到的弹幕交给该接口，由连接管理器统一流转到数字人问答服务。
 */
@FunctionalInterface
public interface BarrageSink {

    void onBarrage(LiveBarrageMessage message);
}
