package com.aoironeon1898.caelum.common.kernel;

/**
 * カーネルによって管理されるタスクのインターフェース。
 * CaelumBlockEntity がこれを実装する。
 */
public interface IBatchTask {

    /**
     * カーネルから呼び出される実行エントリポイント。
     * ここで時間を蓄積(Accumulate)し、1.0以上溜まったらロジックを回す。
     * @param multiplier パーティション数に基づく倍率（例: 4.0）
     */
    void accumulateAndRun(float multiplier);

    /**
     * タスクを実行すべき状態か？
     * (例: チャンクがロードされているか、レッドストーン信号で停止していないか)
     */
    boolean shouldRun();

    /**
     * タスクが完全に無効か？
     * (例: ブロックが破壊された、TileEntityがremovedになった)
     * trueを返すと、カーネルのレジストリから抹消される。
     */
    boolean isInvalidOrUnloaded();
}