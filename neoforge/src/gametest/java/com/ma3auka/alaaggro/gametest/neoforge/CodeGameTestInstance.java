package com.ma3auka.alaaggro.gametest.neoforge;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * A test whose body is a plain method reference.
 *
 * <p>Needed because the vanilla test-function registry is frozen before mod code runs, so the usual
 * function-backed instance is not available to us; registering our own instance type is the way in.
 */
public final class CodeGameTestInstance extends GameTestInstance {

    private final Consumer<GameTestHelper> body;

    public CodeGameTestInstance(Consumer<GameTestHelper> body,
                                TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.body = body;
    }

    @Override
    public void run(GameTestHelper helper) {
        this.body.accept(helper);
    }

    /**
     * The dedicated test server never encodes these, but a real client encodes the whole test
     * registry during its handshake — borrowing another instance type's codec throws there and
     * breaks world loading, so this type carries its own.
     */
    public static final MapCodec<CodeGameTestInstance> CODEC = TestData.CODEC.xmap(
            data -> new CodeGameTestInstance(helper -> { }, data),
            CodeGameTestInstance::info);

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("alaaggro:code");
    }
}
