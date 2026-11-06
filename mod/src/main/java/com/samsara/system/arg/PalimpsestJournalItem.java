package com.samsara.system.arg;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * ARG Feature 1: Tàn Thư Trùng Độc (Palimpsest = bản thảo bị viết đè).
 * Khi dùng (chuột phải), cuốn sổ biến thành sách đã ký "Nhật Ký của Nam":
 * - Lớp mực mới (6 trang): nhật ký của Nam theo Master Production Document - "đừng bấm ngủ",
 *   mặt trời rạn nứt, bầy cừu, Phantom & máy quay, linh cảm "về nhà" sau giấc ngủ thứ 7.
 * - Lớp mực cũ bên dưới (8 trang "Chu Kỳ #0") kể lại hành trình của tiền thân trong The Womb:
 * - Trang 1-5: Tỉnh dậy trong biển cạn, khối cự thạch cắm ngược, rương trôi dạt, con thuyền ma, cái chết vô nghĩa.
 * - Trang 6-7: hành lang cửa save file và lời dặn đi theo dải đèn tới cánh cửa cuối ("đừng ngoảnh lại" - The Wanderer).
 */
public class PalimpsestJournalItem extends Item {

    public PalimpsestJournalItem(Settings settings) {
        super(settings.maxCount(1).rarity(Rarity.RARE));
    }

    public static ItemStack createDecodedBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();

        nbt.putString(WrittenBookItem.TITLE_KEY, "Nhật Ký của Nam");
        nbt.putString(WrittenBookItem.AUTHOR_KEY, "Nam_1998");
        nbt.putInt(WrittenBookItem.GENERATION_KEY, 3); // Tattered / Rách nát

        NbtList pages = new NbtList();

        // ===== LỚP MỰC MỚI: nhật ký của Nam (Video 1-2 trong Master Production Document) =====
        addPage(pages, "samsara.palimpsest_journal.nam_page_1");
        addPage(pages, "samsara.palimpsest_journal.nam_page_2");
        addPage(pages, "samsara.palimpsest_journal.nam_page_3");
        addPage(pages, "samsara.palimpsest_journal.nam_page_4");
        addPage(pages, "samsara.palimpsest_journal.nam_page_5");
        addPage(pages, "samsara.palimpsest_journal.nam_page_6");

        // ===== LỚP MỰC CŨ: Chu Kỳ #0 (gợi ý cho The Womb, giữ nguyên vì các câu đố dựa vào nó) =====

        // Trang 1: Tỉnh thức trong nước cạn
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.palimpsest_journal_item.chu_ky_0_vo"))));

        // Trang 2: Khối nêm cự thạch cắm ngược
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.palimpsest_journal_item.ngay_4_khoi_nem"))));

        // Trang 3: Chiếc rương trôi dạt
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.palimpsest_journal_item.ngay_11_net_chu"))));

        // Trang 4: Ảo ảnh con thuyền ma
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.palimpsest_journal_item.ngay_19_bong_thuyen"))));

        // Trang 5: Nỗi tuyệt vọng về vòng lặp
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.palimpsest_journal_item.ngay_27_cai_chet"))));

        // Trang 6: Những cánh cửa
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.palimpsest_journal_item.ngay_33_nhung_canh"))));

        // Trang 7: Lời dặn cuối cùng
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.palimpsest_journal_item.loi_nhan_cuoi_cung"))));

        nbt.put(WrittenBookItem.PAGES_KEY, pages);
        return book;
    }

    /**
     * "Trang Xé Cuối Cùng" nằm trong chiếc rương đơn độc dưới hầm nhà Nam (chiều Ký Ức Của Nam).
     * Chứa đường dẫn thư mục OBS (Video 3) và chỉ lối xuống vết rách sang The Womb.
     */
    public static ItemStack createLastPage() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString(WrittenBookItem.TITLE_KEY, "Trang Xé Cuối Cùng");
        nbt.putString(WrittenBookItem.AUTHOR_KEY, "Nam_1998");
        nbt.putInt(WrittenBookItem.GENERATION_KEY, 3);

        NbtList pages = new NbtList();
        addPage(pages, "samsara.palimpsest_journal.last_page_1");
        addPage(pages, "samsara.palimpsest_journal.last_page_2");
        addPage(pages, "samsara.palimpsest_journal.last_page_3");
        nbt.put(WrittenBookItem.PAGES_KEY, pages);
        return book;
    }

    private static void addPage(NbtList pages, String key) {
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable(key))));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            ItemStack writtenBook = createDecodedBook();
            user.setStackInHand(hand, writtenBook);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.8f);
            user.sendMessage(Text.translatable("samsara.palimpsest_journal_item.nhung_ky_tu_co"), true);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("samsara.palimpsest_journal_item.cac_trang_giay_da"));
        tooltip.add(Text.translatable("samsara.palimpsest_journal_item.chua_nhung_chuoi_ky"));
    }
}
