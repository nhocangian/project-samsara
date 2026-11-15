package com.samsara.system.arg;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * ARG Feature 3: Chiếc Bục Tự Vấn (The Lectern of Self-Realization).
 * Khi người chơi đặt một cuốn sách đã ký có chứa các câu hỏi triết học về bản ngã:
 * - "Tôi là ai?", "Who am I", "Bản ngã", "Samsara"
 * Mực đen sẽ tự động ăn mòn và trả lời bằng dòng chữ đỏ máu:
 * "Cậu đang hỏi kẻ cầm bút... hay đang hỏi kẻ đang nhìn vào trang giấy này?"
 */
public class SelfRealizationLecternSystem {
    private static final String TAG_ANSWERED = "samsara_answered";

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            BlockEntity be = world.getBlockEntity(hitResult.getBlockPos());
            if (be instanceof LecternBlockEntity lectern) {
                ItemStack book = lectern.getBook();
                if (!book.isEmpty() && book.isOf(Items.WRITTEN_BOOK)) {
                    NbtCompound nbt = book.getNbt();
                    if (nbt != null && !nbt.getBoolean(TAG_ANSWERED)) {
                        String fullContent = extractBookContent(nbt).toLowerCase();

                        if (containsPhilosophicalQuery(fullContent)) {
                            // Mực đen ăn mòn và trả lời
                            nbt.putBoolean(TAG_ANSWERED, true);
                            nbt.putString(WrittenBookItem.AUTHOR_KEY, "Chính Bạn");
                            nbt.putString(WrittenBookItem.TITLE_KEY, "Lời Đáp Của Gương");

                            NbtList newPages = new NbtList();
                            newPages.add(NbtString.of(
                                    "{\"translate\":\"samsara.self_realization_lectern.answer\"}"
                            ));
                            nbt.put(WrittenBookItem.PAGES_KEY, newPages);
                            lectern.markDirty();

                            // Kín đáo: chỉ một tiếng lật trang trầm, không hạt, không chữ báo
                            world.playSound(null, hitResult.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN,
                                    SoundCategory.BLOCKS, 0.8f, 0.5f);
                            return ActionResult.SUCCESS;
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static boolean containsPhilosophicalQuery(String text) {
        return text.contains("tôi là ai") ||
                text.contains("toi la ai") ||
                text.contains("who am i") ||
                text.contains("bản ngã") ||
                text.contains("ban nga") ||
                text.contains("samsara") ||
                text.contains("chính tôi") ||
                text.contains("who is this");
    }

    private static String extractBookContent(NbtCompound nbt) {
        StringBuilder sb = new StringBuilder();
        if (nbt.contains(WrittenBookItem.TITLE_KEY)) {
            sb.append(nbt.getString(WrittenBookItem.TITLE_KEY)).append(" ");
        }
        if (nbt.contains(WrittenBookItem.PAGES_KEY)) {
            NbtList pages = nbt.getList(WrittenBookItem.PAGES_KEY, 8);
            for (int i = 0; i < pages.size(); i++) {
                sb.append(pages.getString(i)).append(" ");
            }
        }
        return sb.toString();
    }
}
