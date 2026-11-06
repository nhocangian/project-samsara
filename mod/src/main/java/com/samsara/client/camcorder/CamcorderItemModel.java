package com.samsara.client.camcorder;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/**
 * Model 3D máy quay cầm tay (thay item phẳng): thân máy, nòng kính + loa che, micro, đèn REC, đèn hồng ngoại,
 * nắp băng (bản lề phía sau, có cuộn băng bên dưới), kính ngắm + cao su mắt, pin sau lưng, dây đeo tay phải,
 * màn LCD lật (bản lề mép trước bên trái).
 *
 * Toạ độ entity (đơn vị pixel, y hướng xuống); renderer dựng gốc ở tâm khối item và scale(-1,-1,1), nên
 * item_x = 8 - x, item_y = 8 - y, item_z = z. Ống kính hướng -Z (ra xa người chơi khi cầm).
 * Texture 64x64 sinh bởi {@code mod/tools/generate_camcorder.py}.
 */
public class CamcorderItemModel {
    public final ModelPart root;
    public final ModelPart lcd;
    public final ModelPart lcdScreen;
    public final ModelPart lid;
    public final ModelPart recLamp;
    public final ModelPart irLamp;

    public CamcorderItemModel() {
        this.root = getTexturedModelData().createModel();
        this.lcd = root.getChild("lcd");
        this.lcdScreen = lcd.getChild("screen");
        this.lid = root.getChild("lid");
        this.recLamp = root.getChild("rec_lamp");
        this.irLamp = root.getChild("ir_lamp");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();
        Dilation none = Dilation.NONE;

        r.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-3, -1.5f, 4, 6, 5, 9, none), ModelTransform.NONE);
        r.addChild("barrel", ModelPartBuilder.create().uv(32, 0).cuboid(-2, -1, 1, 4, 4, 3, none), ModelTransform.NONE);
        r.addChild("hood", ModelPartBuilder.create().uv(48, 0).cuboid(-2.5f, -1.5f, 0, 5, 5, 1, none), ModelTransform.NONE);
        r.addChild("mic", ModelPartBuilder.create().uv(0, 16).cuboid(-1, -2.5f, 2, 2, 1, 3, none), ModelTransform.NONE);
        r.addChild("rec_lamp", ModelPartBuilder.create().uv(12, 16).cuboid(-2.5f, -2.5f, 4, 1, 1, 1, none), ModelTransform.NONE);
        r.addChild("ir_lamp", ModelPartBuilder.create().uv(16, 16).cuboid(-3, -1, 3, 1, 1, 1, none), ModelTransform.NONE);
        // Cuộn băng: tấm phẳng ngay trên mặt thân, nằm gọn trong nắp khi nắp đóng
        r.addChild("cassette", ModelPartBuilder.create().uv(20, 16).cuboid(-2, -1.52f, 7, 4, 0, 3, none), ModelTransform.NONE);
        // Nắp băng: bản lề ở mép sau dưới (z = 11), mở bằng pitch âm (mép trước nhấc lên)
        r.addChild("lid", ModelPartBuilder.create().uv(0, 24).cuboid(-2.5f, -1, -5, 5, 1, 5, none),
                ModelTransform.pivot(0, -1.5f, 11));
        r.addChild("viewfinder", ModelPartBuilder.create().uv(22, 24).cuboid(0, -3.5f, 11, 2, 2, 2, none), ModelTransform.NONE);
        r.addChild("eyecup", ModelPartBuilder.create().uv(32, 24).cuboid(-0.5f, -4, 13, 3, 3, 1, none), ModelTransform.NONE);
        r.addChild("battery", ModelPartBuilder.create().uv(16, 32).cuboid(-2, 0, 13, 4, 3, 1, none), ModelTransform.NONE);
        r.addChild("strap", ModelPartBuilder.create().uv(0, 32).cuboid(-4, -0.5f, 5, 1, 3, 6, none), ModelTransform.NONE);

        // Màn LCD: bản lề ở mép trước bên trái thân (x = 3, z = 5). Gập: nằm áp thân; mở: yaw dương ~95°,
        // mặt trong (có màn) quay về phía người chơi.
        ModelPartData lcd = r.addChild("lcd", ModelPartBuilder.create().uv(32, 8).cuboid(0, -1, 0, 1, 4, 5, none),
                ModelTransform.pivot(3, 0, 5));
        // Mặt màn nằm lùi 0.2 vào trong thân khi gập (bị che), renderer vẽ riêng với ánh sáng tối đa
        lcd.addChild("screen", ModelPartBuilder.create().uv(46, 8).cuboid(-0.2f, -0.5f, 0.5f, 0, 3, 4, none),
                ModelTransform.NONE);

        return TexturedModelData.of(data, 64, 64);
    }
}
