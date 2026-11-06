package com.samsara.client.ending;

import java.util.ArrayList;
import java.util.List;

/**
 * Kịch bản đoạn thoại với <System> (Master Production Document, mục IV), chia thành các bước.
 *  - ME:     nhân vật chính tự gõ vào ô chat rồi gửi (tên hiển thị theo NameMode).
 *  - SYSTEM: <System> trả lời bằng chữ vàng.
 *  - WAIT:   khoảng lặng.
 *  - JUMP:   bóng phản chiếu "nhảy" kích thước + tiếng tim + rung màn hình.
 *  - HATCH:  Quả Trứng nứt thêm (0..1).
 */
public final class EndingDialogue {
    private EndingDialogue() {}

    public enum Kind { ME, SYSTEM, WAIT, JUMP, HATCH }

    /** Tên của nhân vật chính trong chat: Nam_1998 -> nhiễu -> username thật của người chơi. */
    public enum NameMode { NAM, GLITCH, REAL }

    public record Step(Kind kind, String text, NameMode name, float value, int ticks) {
        static Step me(String text) { return new Step(Kind.ME, text, NameMode.NAM, 0, 0); }
        static Step me(NameMode name, String text) { return new Step(Kind.ME, text, name, 0, 0); }
        static Step sys(String text) { return new Step(Kind.SYSTEM, text, NameMode.NAM, 0, 0); }
        static Step wait(int ticks) { return new Step(Kind.WAIT, null, NameMode.NAM, 0, ticks); }
        static Step jump(float scale) { return new Step(Kind.JUMP, null, NameMode.NAM, scale, 0); }
        static Step hatch(float progress) { return new Step(Kind.HATCH, null, NameMode.NAM, progress, 0); }
    }

    /** Bước JUMP cuối cùng: Gargantua hiện ra ở vị trí con mắt. */
    public static final float EYE_REVEAL_SCALE = 45.0f;

    public static List<Step> script() {
        List<Step> s = new ArrayList<>();
        s.add(Step.wait(30));
        s.add(Step.me(net.minecraft.client.resource.language.I18n.translate("samsara.ending.moi_thu_ket_thuc")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.khong_the_gioi_do")));
        s.add(Step.me(net.minecraft.client.resource.language.I18n.translate("samsara.ending.toi_da_chet_tai")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.ban_chi_don_gian")));
        s.add(Step.me(net.minecraft.client.resource.language.I18n.translate("samsara.ending.vay_gia_dinh_toi")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.khong_co_ai_o")));
        s.add(Step.jump(2.0f));
        s.add(Step.me(net.minecraft.client.resource.language.I18n.translate("samsara.ending.y_ban_la_sao")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.tat_ca_ho_deu")));
        s.add(Step.jump(4.0f));
        s.add(Step.hatch(0.15f));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.steve_dau_tien_dat")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.ga_nguoi_choi_bi")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.nguoi_em_trai_ma")));
        s.add(Step.wait(100)); // (Khoảng dừng 5 giây)
        s.add(Step.me(net.minecraft.client.resource.language.I18n.translate("samsara.ending.toi_la_tat_ca")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.la_do_chinh_ban")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.moi_khi_ban_pha")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.moi_khi_ban_giup")));
        s.add(Step.me(net.minecraft.client.resource.language.I18n.translate("samsara.ending.ke_ca_ke_da")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.deu_la_ban_ca")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.ban_da_song_hang")));
        s.add(Step.me(net.minecraft.client.resource.language.I18n.translate("samsara.ending.vay_vu_tru_nay")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.vu_tru_nay_la")));
        s.add(Step.hatch(0.35f));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.ban_la_mot_thai")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.chi_khi_ban_da")));
        s.add(Step.me(NameMode.GLITCH, net.minecraft.client.resource.language.I18n.translate("samsara.ending.sau_khi_truong_thanh")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.ban_se_tro_thanh")));
        s.add(Step.jump(12.0f));
        s.add(Step.wait(160)); // (Khoảng dừng dài)
        s.add(Step.me(NameMode.REAL, net.minecraft.client.resource.language.I18n.translate("samsara.ending.con_nguoi_dang_ngoi")));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.ho_cung_chinh_la")));
        s.add(Step.jump(EYE_REVEAL_SCALE));
        s.add(Step.hatch(0.6f));
        s.add(Step.wait(80));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.nhung_ho_van_dang")));
        s.add(Step.wait(60));
        s.add(Step.sys(net.minecraft.client.resource.language.I18n.translate("samsara.ending.bay_gio_nam_hay")));
        s.add(Step.hatch(1.0f));
        s.add(Step.wait(60));
        return s;
    }
}
