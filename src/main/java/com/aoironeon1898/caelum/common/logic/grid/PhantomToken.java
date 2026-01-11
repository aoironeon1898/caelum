package com.aoironeon1898.caelum.common.logic.grid;

import net.minecraftforge.items.IItemHandler;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

public class PhantomToken {

    private final WeakReference<IItemHandler> targetHandler;
    private final long bornGenerationId;
    private final Supplier<Long> currentGenerationProvider;

    private PhantomToken(IItemHandler handler, long generationId, Supplier<Long> provider) {
        this.targetHandler = new WeakReference<>(handler);
        this.bornGenerationId = generationId;
        this.currentGenerationProvider = provider;
    }

    public static PhantomToken create(IItemHandler handler, long currentGen, Supplier<Long> genProvider) {
        return new PhantomToken(handler, currentGen, genProvider);
    }

    public boolean isValid() {
        return targetHandler.get() != null && bornGenerationId == currentGenerationProvider.get();
    }

    /**
     * ハンドラを取得する。
     * 呼び出し元は必ず isValid() を確認し、かつ null チェックを行うこと。
     */
    @Nullable
    public IItemHandler getHandler() {
        return targetHandler.get();
    }
}