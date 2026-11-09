package com.samsara.registry;

import com.samsara.block.CradleBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry for Project: Samsara blocks.
 */
public class SamsaraBlocks {
    public static final Identifier CRADLE_ID = new Identifier("samsara", "cradle");
    public static final Identifier PRIMORDIAL_EGG_ID = new Identifier("samsara", "primordial_egg");

    // The Cradle block: Unbreakable, high blast resistance, light emission when active
    public static final Block CRADLE = new CradleBlock(
            FabricBlockSettings.copyOf(Blocks.BEDROCK)
                    .strength(-1.0f, 3600000.0f)
                    .luminance(state -> state.get(CradleBlock.ACTIVE) ? 12 : 3)
                    .emissiveLighting((state, world, pos) -> state.get(CradleBlock.ACTIVE))
    );

    // The Primordial Egg: Monolithic altar in The Womb
    public static final Block PRIMORDIAL_EGG = new com.samsara.block.PrimordialEggBlock(
            FabricBlockSettings.copyOf(Blocks.CRYING_OBSIDIAN)
                    .strength(-1.0f, 3600000.0f)
                    .luminance(state -> state.get(com.samsara.block.PrimordialEggBlock.HATCHING) ? 15 : 8)
                    .emissiveLighting((state, world, pos) -> true)
    );

    public static final Identifier MEMORY_SUNFLOWER_ID = new Identifier("samsara", "memory_sunflower");

    // Hoa hướng dương xoay được hướng của chiều Ký Ức Của Nam (không có item riêng)
    public static final Block MEMORY_SUNFLOWER = new com.samsara.block.MemorySunflowerBlock(
            FabricBlockSettings.copyOf(Blocks.SUNFLOWER)
    );

    // Cửa Ký Ức (chương II của The Womb): ba màu phấn như biển cửa trong tham chiếu, không item, không phá được
    public static final Block MEMORY_DOOR_PINK = memoryDoor();
    public static final Block MEMORY_DOOR_BLUE = memoryDoor();
    public static final Block MEMORY_DOOR_WHITE = memoryDoor();

    /** Cánh cửa cuối (chương IV): Cửa Ký Ức trắng cỡ người, mở bằng tay. */
    public static final Block MEMORY_DOOR_FINAL = new com.samsara.block.MemoryDoorFinalBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).nonOpaque().dropsNothing()
            .sounds(net.minecraft.sound.BlockSoundGroup.WOOD));

    /** Bến Đò: máy tính của Nam (màn hình là ảnh chụp mặt trời có chấm đen), tự sáng nhẹ. */
    public static final Block NAM_COMPUTER = new com.samsara.block.DockPropBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).nonOpaque().dropsNothing().luminance(state -> 6)
            .sounds(net.minecraft.sound.BlockSoundGroup.STONE),
            net.minecraft.block.Block.createCuboidShape(2, 0, 3, 14, 12, 16));

    /** Bến Đò: mảng tường phòng ngủ của Nam (giấy dán tường / ván sồi). */
    public static final Block NAM_WALLPAPER = new com.samsara.block.NamWallpaperBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)
            .strength(-1.0f, 3600000.0f).dropsNothing());

    /** Bến Đò: dấu chân ướt in trên sàn (decal, không va chạm). */
    public static final Block WET_FOOTPRINTS = new com.samsara.block.DockPropBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).noCollision().nonOpaque().dropsNothing(),
            net.minecraft.block.Block.createCuboidShape(0, 0, 0, 16, 0.25, 16));

    /** Tầng Dưới: gạch men trắng ố lát Đường Ống. */
    public static final Block GACH_MEN_O = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
            .strength(-1.0f, 3600000.0f).dropsNothing());
    /** Tầng Dưới: dấu chân ướt trên gờ gạch, cùng hình với dấu chân ở Bến Đò nhưng màu xám. */
    public static final Block DAU_CHAN_GACH = new com.samsara.block.DockPropBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).noCollision().nonOpaque().dropsNothing(),
            net.minecraft.block.Block.createCuboidShape(0, 0, 0, 16, 0.25, 16));
    /** Tầng Dưới: ô thiếu texture, càng xuống các tầng càng nhiều. */
    public static final Block KHOI_CHUA_TAI = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
            .strength(-1.0f, 3600000.0f).dropsNothing());
    /** Tầng Dưới: bánh số đá của ổ khoá ở căn hầm. */
    public static final Block BANH_KHOA = new com.samsara.block.BanhKhoaBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)
            .strength(-1.0f, 3600000.0f).dropsNothing());
    /** Tầng Dưới: gạch trần đang rỉ nước. */
    public static final Block GACH_MEN_RI = new com.samsara.block.GachMenRiBlock(FabricBlockSettings.copyOf(Blocks.STONE)
            .strength(-1.0f, 3600000.0f).dropsNothing());
    /** Tầng Dưới: gạch men thỉnh thoảng bị xé lát, tách màu (texture động). */
    public static final Block GACH_MEN_GLITCH = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
            .strength(-1.0f, 3600000.0f).dropsNothing());
    /** Tầng Dưới: đèn huỳnh quang trên trần ống. */
    public static final Block DEN_HUYNH_QUANG = new com.samsara.block.DenOngBlock(FabricBlockSettings.copyOf(Blocks.STONE)
            .strength(-1.0f, 3600000.0f).dropsNothing().luminance(state -> 15));

    /** Tầng Dưới: hoa hướng dương đã khô (ngã 3 của tầng 1); hình dáng và FACING như hoa thường. */
    public static final Block HOA_KHO = new com.samsara.block.MemorySunflowerBlock(
            FabricBlockSettings.copyOf(Blocks.SUNFLOWER).dropsNothing());

    /** Tầng Dưới: đèn chập chờn, sáng khi LIT. */
    public static final Block DEN_CHOP = new com.samsara.block.DenChopBlock(FabricBlockSettings.copyOf(Blocks.STONE)
            .strength(-1.0f, 3600000.0f).dropsNothing()
            .luminance(state -> state.get(com.samsara.block.DenChopBlock.LIT) ? 15 : 0));
    /** Tầng Dưới: đèn hỏng hẳn, bắn tia lửa. */
    public static final Block DEN_HONG = new com.samsara.block.DenHongBlock(FabricBlockSettings.copyOf(Blocks.STONE)
            .strength(-1.0f, 3600000.0f).dropsNothing());

    /** Tầng Dưới, tầng 2: Khối Nháp, đứng được nhưng chỉ thấy qua máy quay. */
    public static final Block KHOI_NHAP = new com.samsara.block.KhoiNhapBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).nonOpaque().dropsNothing()
            .sounds(net.minecraft.sound.BlockSoundGroup.AMETHYST_CLUSTER), true);
    /** Tầng 2, đoạn 4: cầu ván chỉ có trong mắt, đi qua được, nhìn qua máy quay thì không còn. */
    public static final Block VAN_GIA = new com.samsara.block.KhoiNhapBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).nonOpaque().noCollision().dropsNothing(), false);

    /** Lớp vỏ glitch phủ lên vật thể ở Bến Đò (chỉ dùng trong block display). */
    public static final Block WOMB_GLITCH = new com.samsara.block.WombGlitchBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).noCollision().nonOpaque().dropsNothing());

    /** Khối Mã: block đen có mã nhị phân xanh rơi, các khối cạnh nhau nối liền (Bến Đò). */
    public static final Block WOMB_CODE = new com.samsara.block.WombCodeBlock(FabricBlockSettings.copyOf(Blocks.OBSIDIAN)
            .strength(-1.0f, 3600000.0f).dropsNothing().luminance(state -> 5)
            .mapColor(net.minecraft.block.MapColor.BLACK));

    /** Cánh cửa trắng khổng lồ hé mở ở cuối hành lang cửa (vẽ bằng MemoryDoorGiantRenderer). */
    public static final Block MEMORY_DOOR_GIANT = new com.samsara.block.MemoryDoorGiantBlock(
            FabricBlockSettings.create().strength(-1.0f, 3600000.0f).noCollision().nonOpaque().dropsNothing()
                    .luminance(state -> 15));

    /** Vách Hư Không: tường và trần đen tuyền của căn phòng bao Bến Đò và biển cửa (chặn cả ánh trời). */
    public static final Block WOMB_VEIL = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK)
            .strength(-1.0f, 3600000.0f).dropsNothing().mapColor(net.minecraft.block.MapColor.BLACK));

    /** Tượng Quỳ (chương III của The Womb): thạch cao trắng, không item, không phá được. */
    public static final Block MEMORY_STATUE = new com.samsara.block.MemoryStatueBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f).nonOpaque().dropsNothing()
            .sounds(net.minecraft.sound.BlockSoundGroup.CALCITE));

    /** Vầng hào quang + tia sáng của khổng thần (vẽ bởi TitanHaloRenderer). */
    public static final Block TITAN_HALO = new com.samsara.block.TitanHaloBlock(
            FabricBlockSettings.create().strength(-1.0f, 3600000.0f).noCollision().nonOpaque().dropsNothing());

    private static Block memoryDoor() {
        return new com.samsara.block.MemoryDoorBlock(FabricBlockSettings.create()
                .strength(-1.0f, 3600000.0f).nonOpaque().dropsNothing()
                .sounds(net.minecraft.sound.BlockSoundGroup.WOOD));
    }

    public static void registerBlocks() {
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_pink"), MEMORY_DOOR_PINK);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_blue"), MEMORY_DOOR_BLUE);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_white"), MEMORY_DOOR_WHITE);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_giant"), MEMORY_DOOR_GIANT);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_door_final"), MEMORY_DOOR_FINAL);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "nam_computer"), NAM_COMPUTER);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "nam_wallpaper"), NAM_WALLPAPER);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "wet_footprints"), WET_FOOTPRINTS);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "gach_men_o"), GACH_MEN_O);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "dau_chan_gach"), DAU_CHAN_GACH);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "khoi_chua_tai"), KHOI_CHUA_TAI);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "banh_khoa"), BANH_KHOA);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "gach_men_ri"), GACH_MEN_RI);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "gach_men_glitch"), GACH_MEN_GLITCH);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "den_huynh_quang"), DEN_HUYNH_QUANG);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "hoa_kho"), HOA_KHO);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "den_chop"), DEN_CHOP);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "den_hong"), DEN_HONG);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "khoi_nhap"), KHOI_NHAP);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "van_gia"), VAN_GIA);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "womb_glitch"), WOMB_GLITCH);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "womb_code"), WOMB_CODE);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "womb_veil"), WOMB_VEIL);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "memory_statue"), MEMORY_STATUE);
        Registry.register(Registries.BLOCK, new Identifier("samsara", "titan_halo"), TITAN_HALO);
        Registry.register(Registries.BLOCK, MEMORY_SUNFLOWER_ID, MEMORY_SUNFLOWER);
        Registry.register(Registries.BLOCK, CRADLE_ID, CRADLE);
        Registry.register(Registries.BLOCK, PRIMORDIAL_EGG_ID, PRIMORDIAL_EGG);
    }
}
