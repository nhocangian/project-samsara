package com.samsara.mixin;

import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Cho UiHorrorSystem đổi tên thế giới (LevelName trong level.dat) trước khi game lưu lúc thoát. */
@Mixin(LevelProperties.class)
public interface LevelPropertiesAccessor {
    @Accessor("levelInfo")
    void samsara$setLevelInfo(LevelInfo info);
}
