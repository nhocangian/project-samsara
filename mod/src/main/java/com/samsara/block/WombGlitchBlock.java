package com.samsara.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/**
 * Lớp vỏ glitch (texture động, phần lớn trong suốt): không đặt như block thường, DockDrift phủ nó lên vật thể bằng
 * block display phóng to nhỉnh hơn khối gốc. Mỗi biến thể là chính texture của vật bị xé lát, tách kênh màu:
 * chất liệu = VARIANT / 2: 0 giấy dán tường, 1 ván sồi, 2 len đỏ (giường), 3 gỗ vân sam bóc vỏ (khung cửa); mỗi chất
 * liệu 2 nhịp (VARIANT chẵn / lẻ) nên các tấm cùng loại không giật cùng lúc. Sinh bởi tools/generate_glitch.py.
 */
public class WombGlitchBlock extends Block {
    public static final IntProperty VARIANT = IntProperty.of("variant", 0, 7);

    public WombGlitchBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(VARIANT, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }
}
